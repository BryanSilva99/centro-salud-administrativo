import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import axe from 'axe-core';
import { DisponibilidadPanel } from '../src/features/administration/components/DisponibilidadPanel';
import { TurnoForm } from '../src/features/administration/components/TurnoForm';
import { SessionView } from '../src/features/identity/components/SessionView';
import type { Turno } from '../src/features/administration/api/disponibilidad';
import type { Cuenta } from '../src/features/identity/api/auth';
const response=(status: number,body: unknown)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
const profesional={id:'p1',identificacion:'P-01',nombre:'Profesional de prueba',funcion:'Medicina',activa:true,version:0,serviciosHabilitados:[]};
const turno: Turno={id:'t1',fecha:'2026-10-15',horaInicio:'08:00:00',horaFin:'12:00:00',tipo:'DISPONIBLE',activo:true};
function setup(write: (url: string, options: RequestInit) => Response, turnos: Turno[]=[turno]) {
  const fetch=vi.fn(async(url: string,options: RequestInit={})=>{
    if(url.endsWith('/csrf'))return response(200,{headerName:'X-CSRF-TOKEN',token:'csrf'});
    if(options.method==='POST'||options.method==='PUT')return write(url,options);
    if(url==='/api/profesionales/fichas')return response(200,[profesional]);
    if(url.startsWith('/api/disponibilidad/'))return response(200,turnos);
    return response(404,{});
  });vi.stubGlobal('fetch',fetch);return fetch;
}
describe('Disponibilidad',()=>{
  it('crea un turno con el intervalo indicado',async()=>{
    const save=vi.fn((url: string,options: RequestInit)=>{
      expect(url).toBe('/api/disponibilidad/profesionales/p1');expect(options.headers).toMatchObject({'X-CSRF-TOKEN':'csrf'});
      expect(JSON.parse(options.body as string)).toEqual({fecha:'2026-10-20',horaInicio:'13:00',horaFin:'17:00',tipo:'DESCANSO'});
      return response(201,{...turno,id:'t2',fecha:'2026-10-20',horaInicio:'13:00:00',horaFin:'17:00:00',tipo:'DESCANSO'});
    });setup(save);
    const onSaved=vi.fn();
    render(<TurnoForm tipo="profesionales" recursoId="p1" onSaved={onSaved} onExpired={vi.fn()}/>);
    fireEvent.change(screen.getByLabelText('Fecha'),{target:{value:'2026-10-20'}});
    fireEvent.change(screen.getByLabelText('Hora de inicio'),{target:{value:'13:00'}});
    fireEvent.change(screen.getByLabelText('Hora de fin'),{target:{value:'17:00'}});
    fireEvent.change(screen.getByLabelText('Tipo'),{target:{value:'DESCANSO'}});
    fireEvent.click(screen.getByRole('button',{name:'Crear turno'}));
    await waitFor(()=>expect(onSaved).toHaveBeenCalled());expect(save).toHaveBeenCalledTimes(1);
  });
  it('explica un turno superpuesto y no repite el envío',async()=>{
    const save=vi.fn(()=>response(409,{code:'SUPERPUESTO'}));setup(save);
    render(<TurnoForm tipo="profesionales" recursoId="p1" onSaved={vi.fn()} onExpired={vi.fn()}/>);
    fireEvent.click(screen.getByRole('button',{name:'Crear turno'}));
    expect(await screen.findByRole('alert')).toHaveTextContent('se cruza');expect(save).toHaveBeenCalledTimes(1);
  });
  it('lista los turnos del recurso seleccionado y permite desactivarlos',async()=>{
    const desactivar=vi.fn((_url: string,_options: RequestInit)=>response(200,{}));setup(desactivar);
    render(<DisponibilidadPanel onExpired={vi.fn()}/>);
    fireEvent.change(await screen.findByLabelText('Recurso'),{target:{value:'p1'}});
    expect(await screen.findByText(/2026-10-15.*08:00:00–12:00:00.*Disponible.*Activo/)).toBeVisible();
    fireEvent.click(screen.getByRole('button',{name:'Desactivar'}));
    await waitFor(()=>expect(desactivar).toHaveBeenCalledTimes(1));
    const [url,options]=desactivar.mock.calls[0];
    expect(url).toBe('/api/disponibilidad/profesionales/p1/t1');expect(options.method).toBe('PUT');
    expect(JSON.parse(options.body as string)).toEqual({activo:false});
  });
  it('cambia de tipo de recurso y vuelve a cargar la lista',async()=>{
    setup(()=>response(200,{}));
    render(<DisponibilidadPanel onExpired={vi.fn()}/>);
    await screen.findByLabelText('Consulta general', {exact:false}).catch(()=>{});
    fireEvent.change(screen.getByLabelText('Tipo de recurso'),{target:{value:'consultorios'}});
    expect(screen.getByLabelText('Recurso')).toHaveValue('');
  });
  it('solo ofrece la gestión a cuentas con permiso',()=>{
    setup(()=>response(200,[turno]));
    const base={id:'c',identificador:'cuenta',activa:true} as const;
    const {unmount}=render(<SessionView cuenta={{...base,roles:['ADM'],permisos:['PROFESIONALES_GESTIONAR']} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);
    expect(screen.queryByRole('button',{name:'Gestionar disponibilidad'})).not.toBeInTheDocument();unmount();
    render(<SessionView cuenta={{...base,roles:['ADM'],permisos:['DISPONIBILIDAD_GESTIONAR']} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);
    expect(screen.getByRole('button',{name:'Gestionar disponibilidad'})).toBeVisible();
  });
  it('presenta formularios accesibles en estado inicial y de error',async()=>{
    setup(()=>response(409,{code:'SUPERPUESTO'}));
    const {container}=render(<main><TurnoForm tipo="profesionales" recursoId="p1" onSaved={vi.fn()} onExpired={vi.fn()}/></main>);
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
    fireEvent.click(screen.getByRole('button',{name:'Crear turno'}));await screen.findByRole('alert');
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
  });
});
