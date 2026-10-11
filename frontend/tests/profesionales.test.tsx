import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import axe from 'axe-core';
import { ProfesionalForm } from '../src/features/administration/components/ProfesionalForm';
import { ProfesionalesPanel } from '../src/features/administration/components/ProfesionalesPanel';
import { SessionView } from '../src/features/identity/components/SessionView';
import type { FichaProfesional } from '../src/features/administration/api/profesionales';
import type { Cuenta } from '../src/features/identity/api/auth';
const response=(status: number,body: unknown)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
const servicio={id:'srv1',nombre:'Consulta general',duracionMinutos:30,activo:true,version:0,modalidades:[]};
const ficha: FichaProfesional={id:'p1',identificacion:'P-01',nombre:'Profesional de prueba',funcion:'Medicina general',activa:true,version:2,serviciosHabilitados:['srv1']};
function setup(write: (url: string, options: RequestInit) => Response, lista: FichaProfesional[]=[ficha]) {
  const fetch=vi.fn(async(url: string,options: RequestInit={})=>{
    if(url.endsWith('/csrf'))return response(200,{headerName:'X-CSRF-TOKEN',token:'csrf'});
    if(options.method==='POST'||options.method==='PUT')return write(url,options);
    if(url==='/api/servicios')return response(200,[servicio]);
    if(url==='/api/profesionales/fichas')return response(200,lista);
    return response(404,{});
  });vi.stubGlobal('fetch',fetch);return fetch;
}
const props=()=>({onSaved:vi.fn(),onReload:vi.fn(),onExpired:vi.fn()});
describe('Profesionales',()=>{
  it('crea una ficha con los servicios habilitados seleccionados',async()=>{
    const save=vi.fn((url: string,options: RequestInit)=>{
      expect(url).toBe('/api/profesionales/fichas');expect(options.headers).toMatchObject({'X-CSRF-TOKEN':'csrf'});
      expect(JSON.parse(options.body as string)).toEqual({identificacion:'P-02',nombre:'Otro profesional',funcion:'Enfermería',serviciosHabilitados:['srv1']});
      return response(201,ficha);
    });setup(save);const p=props();
    render(<ProfesionalForm ficha={null} {...p}/>);
    fireEvent.change(screen.getByLabelText('Identificación'),{target:{value:' P-02 '}});
    fireEvent.change(screen.getByLabelText('Nombre completo'),{target:{value:' Otro profesional '}});
    fireEvent.change(screen.getByLabelText('Especialidad o función'),{target:{value:' Enfermería '}});
    fireEvent.click(await screen.findByLabelText('Consulta general'));
    fireEvent.click(screen.getByRole('button',{name:'Crear ficha'}));
    await waitFor(()=>expect(p.onSaved).toHaveBeenCalledWith(ficha));expect(save).toHaveBeenCalledTimes(1);
  });
  it('edita con la versión leída y preselecciona los servicios ya habilitados',async()=>{
    setup((url,options)=>{
      expect(url).toBe('/api/profesionales/fichas/p1');expect(options.method).toBe('PUT');
      expect(JSON.parse(options.body as string)).toEqual({identificacion:'P-01',nombre:'Profesional de prueba',funcion:'Medicina general',serviciosHabilitados:[],activa:false,version:2});
      return response(200,{...ficha,activa:false,version:3,serviciosHabilitados:[]});
    });const p=props();
    render(<ProfesionalForm ficha={ficha} {...p}/>);
    expect(await screen.findByLabelText('Consulta general')).toBeChecked();
    fireEvent.click(screen.getByLabelText('Consulta general'));
    fireEvent.click(screen.getByLabelText('Ficha activa'));
    fireEvent.click(screen.getByRole('button',{name:'Guardar ficha'}));
    await waitFor(()=>expect(p.onSaved).toHaveBeenCalledWith({...ficha,activa:false,version:3,serviciosHabilitados:[]}));
  });
  it('ante una edición obsoleta bloquea el guardado y ofrece recargar',async()=>{
    const save=vi.fn(()=>response(409,{code:'CONFLICTO'}));setup(save);const p=props();
    render(<ProfesionalForm ficha={ficha} {...p}/>);
    fireEvent.click(screen.getByRole('button',{name:'Guardar ficha'}));
    expect(await screen.findByRole('alert')).toHaveTextContent('Recárgala');
    expect(screen.getByRole('button',{name:'Guardar ficha'})).toBeDisabled();
    fireEvent.click(screen.getByRole('button',{name:'Recargar ficha'}));expect(p.onReload).toHaveBeenCalled();expect(save).toHaveBeenCalledTimes(1);
  });
  it('explica una identificación duplicada y no repite el envío',async()=>{
    const save=vi.fn(()=>response(409,{}));setup(save);
    render(<ProfesionalForm ficha={null} {...props()}/>);
    fireEvent.change(screen.getByLabelText('Identificación'),{target:{value:'P-01'}});
    fireEvent.change(screen.getByLabelText('Nombre completo'),{target:{value:'Profesional'}});
    fireEvent.change(screen.getByLabelText('Especialidad o función'),{target:{value:'Medicina'}});
    fireEvent.click(screen.getByRole('button',{name:'Crear ficha'}));
    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe una ficha');expect(save).toHaveBeenCalledTimes(1);
  });
  it('lista las fichas y abre la seleccionada para editar',async()=>{
    setup(()=>response(200,ficha),[ficha,{...ficha,id:'p2',identificacion:'P-02',activa:false}]);
    render(<ProfesionalesPanel onExpired={vi.fn()}/>);
    expect(await screen.findByRole('button',{name:'P-02 · Profesional de prueba · Inactivo'})).toBeVisible();
    fireEvent.click(screen.getByRole('button',{name:'P-01 · Profesional de prueba · Activo'}));
    expect(screen.getByRole('form',{name:'Editar P-01'})).toBeVisible();
  });
  it('solo ofrece la gestión a cuentas con permiso',()=>{
    setup(()=>response(200,ficha));
    const base={id:'c',identificador:'cuenta',activa:true} as const;
    const {unmount}=render(<SessionView cuenta={{...base,roles:['REC'],permisos:['PACIENTES_GESTIONAR']} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);
    expect(screen.queryByRole('button',{name:'Gestionar profesionales'})).not.toBeInTheDocument();unmount();
    render(<SessionView cuenta={{...base,roles:['ADM'],permisos:['PROFESIONALES_GESTIONAR']} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);
    expect(screen.getByRole('button',{name:'Gestionar profesionales'})).toBeVisible();
  });
  it('presenta formularios accesibles de alta, edición y error',async()=>{
    setup(()=>response(409,{code:'CONFLICTO'}));
    const {container,rerender}=render(<main><ProfesionalForm ficha={null} {...props()}/></main>);
    await screen.findByLabelText('Consulta general');
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
    rerender(<main><ProfesionalForm key="edicion" ficha={ficha} {...props()}/></main>);
    fireEvent.click(screen.getByRole('button',{name:'Guardar ficha'}));await screen.findByRole('alert');
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
  });
});
