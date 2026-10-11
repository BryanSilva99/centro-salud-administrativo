import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import axe from 'axe-core';
import { ServicioForm } from '../src/features/administration/components/ServicioForm';
import { ServiciosPanel } from '../src/features/administration/components/ServiciosPanel';
import { SessionView } from '../src/features/identity/components/SessionView';
import type { Servicio } from '../src/features/administration/api/servicios';
import type { Cuenta } from '../src/features/identity/api/auth';
const response=(status: number,body: unknown)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
const servicio: Servicio={id:'s1',nombre:'Consulta general',duracionMinutos:30,activo:true,version:2,modalidades:[
  {id:'m1',nombre:'Particular',conCargo:true,tarifa:40,activa:true},{id:'m2',nombre:'Campaña',conCargo:false,tarifa:0,activa:true}]};
function setup(write: (url: string, options: RequestInit) => Response, lista: Servicio[]=[servicio]) {
  const fetch=vi.fn(async(url: string,options: RequestInit={})=>{
    if(url.endsWith('/csrf'))return response(200,{headerName:'X-CSRF-TOKEN',token:'csrf'});
    if(options.method==='POST'||options.method==='PUT')return write(url,options);
    if(url==='/api/servicios')return response(200,lista);
    return response(404,{});
  });vi.stubGlobal('fetch',fetch);return fetch;
}
const props=()=>({onSaved:vi.fn(),onReload:vi.fn(),onExpired:vi.fn()});
const modalidad=(n: number)=>within(screen.getByRole('group',{name:`Modalidad ${n}`}));
describe('Servicios y condiciones económicas',()=>{
  it('crea un servicio con modalidades enviando la tarifa como texto exacto',async()=>{
    const save=vi.fn((url: string,options: RequestInit)=>{
      expect(url).toBe('/api/servicios');expect(options.headers).toMatchObject({'X-CSRF-TOKEN':'csrf'});
      expect(JSON.parse(options.body as string)).toEqual({nombre:'Ecografía',duracionMinutos:45,modalidades:[
        {id:null,nombre:'Particular',conCargo:true,tarifa:'40.50',activa:true},{id:null,nombre:'Campaña',conCargo:false,tarifa:'0',activa:true}]});
      return response(201,servicio);
    });setup(save);const p=props();
    render(<ServicioForm servicio={null} {...p}/>);
    fireEvent.change(screen.getByLabelText('Nombre del servicio'),{target:{value:' Ecografía '}});
    fireEvent.change(screen.getByLabelText('Duración en minutos'),{target:{value:'45'}});
    fireEvent.change(modalidad(1).getByLabelText('Nombre de la modalidad'),{target:{value:'Particular'}});
    fireEvent.change(modalidad(1).getByLabelText('Tarifa en soles (S/)'),{target:{value:'40.50'}});
    fireEvent.click(screen.getByRole('button',{name:'Agregar modalidad'}));
    fireEvent.change(modalidad(2).getByLabelText('Nombre de la modalidad'),{target:{value:'Campaña'}});
    fireEvent.click(modalidad(2).getByLabelText('Con cargo'));
    expect(modalidad(2).getByLabelText('Tarifa en soles (S/)')).toBeDisabled();
    fireEvent.click(screen.getByRole('button',{name:'Crear servicio'}));
    await waitFor(()=>expect(p.onSaved).toHaveBeenCalledWith(servicio));expect(save).toHaveBeenCalledTimes(1);
  });
  it('edita con la versión leída y solo permite desactivar modalidades guardadas',async()=>{
    setup((url,options)=>{
      expect(url).toBe('/api/servicios/s1');expect(options.method).toBe('PUT');
      expect(JSON.parse(options.body as string)).toEqual({nombre:'Consulta general',duracionMinutos:30,activo:true,version:2,modalidades:[
        {id:'m1',nombre:'Particular',conCargo:true,tarifa:'45.00',activa:true},{id:'m2',nombre:'Campaña',conCargo:false,tarifa:'0',activa:false}]});
      return response(200,{...servicio,version:3});
    });const p=props();
    render(<ServicioForm servicio={servicio} {...p}/>);
    expect(modalidad(1).getByLabelText('Tarifa en soles (S/)')).toHaveValue('40.00');
    expect(screen.queryByRole('button',{name:/Quitar modalidad/})).not.toBeInTheDocument();
    fireEvent.change(modalidad(1).getByLabelText('Tarifa en soles (S/)'),{target:{value:'45.00'}});
    fireEvent.click(modalidad(2).getByLabelText('Modalidad activa'));
    fireEvent.click(screen.getByRole('button',{name:'Guardar servicio'}));
    await waitFor(()=>expect(p.onSaved).toHaveBeenCalledWith({...servicio,version:3}));
  });
  it('ante una edición obsoleta bloquea el guardado y ofrece recargar',async()=>{
    const save=vi.fn(()=>response(409,{code:'CONFLICTO'}));setup(save);const p=props();
    render(<ServicioForm servicio={servicio} {...p}/>);
    fireEvent.click(screen.getByRole('button',{name:'Guardar servicio'}));
    expect(await screen.findByRole('alert')).toHaveTextContent('Recárgalo');
    expect(screen.getByRole('button',{name:'Guardar servicio'})).toBeDisabled();
    fireEvent.click(screen.getByRole('button',{name:'Recargar servicio'}));expect(p.onReload).toHaveBeenCalled();expect(save).toHaveBeenCalledTimes(1);
  });
  it('explica un nombre duplicado y no repite el envío',async()=>{
    const save=vi.fn(()=>response(409,{code:'NOMBRE_DUPLICADO'}));setup(save);
    render(<ServicioForm servicio={null} {...props()}/>);
    fireEvent.change(screen.getByLabelText('Nombre del servicio'),{target:{value:'Consulta general'}});
    fireEvent.change(screen.getByLabelText('Duración en minutos'),{target:{value:'30'}});
    fireEvent.change(modalidad(1).getByLabelText('Nombre de la modalidad'),{target:{value:'Particular'}});
    fireEvent.change(modalidad(1).getByLabelText('Tarifa en soles (S/)'),{target:{value:'40'}});
    fireEvent.click(screen.getByRole('button',{name:'Crear servicio'}));
    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe un servicio');expect(save).toHaveBeenCalledTimes(1);
  });
  it('lista los servicios y abre el seleccionado para editar',async()=>{
    setup(()=>response(200,servicio),[servicio,{...servicio,id:'s2',nombre:'Ecografía',activo:false}]);
    render(<ServiciosPanel onExpired={vi.fn()}/>);
    expect(await screen.findByRole('button',{name:'Ecografía · 30 min · Inactivo'})).toBeVisible();
    fireEvent.click(screen.getByRole('button',{name:'Consulta general · 30 min · Activo'}));
    expect(screen.getByRole('form',{name:'Editar Consulta general'})).toBeVisible();
  });
  it('solo ofrece la gestión a cuentas con permiso',()=>{
    setup(()=>response(200,servicio));
    const base={id:'c',identificador:'cuenta',activa:true} as const;
    const {unmount}=render(<SessionView cuenta={{...base,roles:['REC'],permisos:['PACIENTES_GESTIONAR']} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);
    expect(screen.queryByRole('button',{name:'Gestionar servicios'})).not.toBeInTheDocument();unmount();
    render(<SessionView cuenta={{...base,roles:['ADM'],permisos:['SERVICIOS_GESTIONAR']} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);
    expect(screen.getByRole('button',{name:'Gestionar servicios'})).toBeVisible();
  });
  it('presenta formularios accesibles de alta, edición y error',async()=>{
    setup(()=>response(409,{code:'CONFLICTO'}));
    const {container,rerender}=render(<main><ServicioForm servicio={null} {...props()}/></main>);
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
    rerender(<main><ServicioForm key="edicion" servicio={servicio} {...props()}/></main>);
    fireEvent.click(screen.getByRole('button',{name:'Guardar servicio'}));await screen.findByRole('alert');
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
  });
});
