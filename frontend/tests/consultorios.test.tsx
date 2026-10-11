import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import axe from 'axe-core';
import { ConsultorioForm } from '../src/features/administration/components/ConsultorioForm';
import { ConsultoriosPanel } from '../src/features/administration/components/ConsultoriosPanel';
import { SessionView } from '../src/features/identity/components/SessionView';
import type { Consultorio } from '../src/features/administration/api/consultorios';
import type { Cuenta } from '../src/features/identity/api/auth';
const response=(status: number,body: unknown)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
const servicio={id:'srv1',nombre:'Consulta general',duracionMinutos:30,activo:true,version:0,modalidades:[]};
const consultorio: Consultorio={id:'c1',codigo:'C-100',nombre:'Consultorio 1',activo:true,version:2,serviciosCompatibles:['srv1']};
function setup(write: (url: string, options: RequestInit) => Response, lista: Consultorio[]=[consultorio]) {
  const fetch=vi.fn(async(url: string,options: RequestInit={})=>{
    if(url.endsWith('/csrf'))return response(200,{headerName:'X-CSRF-TOKEN',token:'csrf'});
    if(options.method==='POST'||options.method==='PUT')return write(url,options);
    if(url==='/api/servicios')return response(200,[servicio]);
    if(url==='/api/consultorios')return response(200,lista);
    return response(404,{});
  });vi.stubGlobal('fetch',fetch);return fetch;
}
const props=()=>({onSaved:vi.fn(),onReload:vi.fn(),onExpired:vi.fn()});
describe('Consultorios',()=>{
  it('crea un consultorio con los servicios compatibles seleccionados',async()=>{
    const save=vi.fn((url: string,options: RequestInit)=>{
      expect(url).toBe('/api/consultorios');expect(options.headers).toMatchObject({'X-CSRF-TOKEN':'csrf'});
      expect(JSON.parse(options.body as string)).toEqual({codigo:'C-200',nombre:'Consultorio 2',serviciosCompatibles:['srv1']});
      return response(201,consultorio);
    });setup(save);const p=props();
    render(<ConsultorioForm consultorio={null} {...p}/>);
    fireEvent.change(screen.getByLabelText('Código del consultorio'),{target:{value:' C-200 '}});
    fireEvent.change(screen.getByLabelText('Nombre'),{target:{value:' Consultorio 2 '}});
    fireEvent.click(await screen.findByLabelText('Consulta general'));
    fireEvent.click(screen.getByRole('button',{name:'Crear consultorio'}));
    await waitFor(()=>expect(p.onSaved).toHaveBeenCalledWith(consultorio));expect(save).toHaveBeenCalledTimes(1);
  });
  it('edita con la versión leída y preselecciona los servicios ya compatibles',async()=>{
    setup((url,options)=>{
      expect(url).toBe('/api/consultorios/c1');expect(options.method).toBe('PUT');
      expect(JSON.parse(options.body as string)).toEqual({codigo:'C-100',nombre:'Consultorio 1',serviciosCompatibles:[],activo:false,version:2});
      return response(200,{...consultorio,activo:false,version:3,serviciosCompatibles:[]});
    });const p=props();
    render(<ConsultorioForm consultorio={consultorio} {...p}/>);
    expect(await screen.findByLabelText('Consulta general')).toBeChecked();
    fireEvent.click(screen.getByLabelText('Consulta general'));
    fireEvent.click(screen.getByLabelText('Consultorio activo'));
    fireEvent.click(screen.getByRole('button',{name:'Guardar consultorio'}));
    await waitFor(()=>expect(p.onSaved).toHaveBeenCalledWith({...consultorio,activo:false,version:3,serviciosCompatibles:[]}));
  });
  it('ante una edición obsoleta bloquea el guardado y ofrece recargar',async()=>{
    const save=vi.fn(()=>response(409,{code:'CONFLICTO'}));setup(save);const p=props();
    render(<ConsultorioForm consultorio={consultorio} {...p}/>);
    fireEvent.click(screen.getByRole('button',{name:'Guardar consultorio'}));
    expect(await screen.findByRole('alert')).toHaveTextContent('Recárgalo');
    expect(screen.getByRole('button',{name:'Guardar consultorio'})).toBeDisabled();
    fireEvent.click(screen.getByRole('button',{name:'Recargar consultorio'}));expect(p.onReload).toHaveBeenCalled();expect(save).toHaveBeenCalledTimes(1);
  });
  it('explica un código duplicado y no repite el envío',async()=>{
    const save=vi.fn(()=>response(409,{code:'CODIGO_DUPLICADO'}));setup(save);
    render(<ConsultorioForm consultorio={null} {...props()}/>);
    fireEvent.change(screen.getByLabelText('Código del consultorio'),{target:{value:'C-100'}});
    fireEvent.change(screen.getByLabelText('Nombre'),{target:{value:'Consultorio'}});
    fireEvent.click(screen.getByRole('button',{name:'Crear consultorio'}));
    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe un consultorio');expect(save).toHaveBeenCalledTimes(1);
  });
  it('lista los consultorios y abre el seleccionado para editar',async()=>{
    setup(()=>response(200,consultorio),[consultorio,{...consultorio,id:'c2',codigo:'C-200',activo:false}]);
    render(<ConsultoriosPanel onExpired={vi.fn()}/>);
    expect(await screen.findByRole('button',{name:'C-200 · Consultorio 1 · Inactivo'})).toBeVisible();
    fireEvent.click(screen.getByRole('button',{name:'C-100 · Consultorio 1 · Activo'}));
    expect(screen.getByRole('form',{name:'Editar C-100'})).toBeVisible();
  });
  it('solo ofrece la gestión a cuentas con permiso',()=>{
    setup(()=>response(200,consultorio));
    const base={id:'c',identificador:'cuenta',activa:true} as const;
    const {unmount}=render(<SessionView cuenta={{...base,roles:['REC'],permisos:['PACIENTES_GESTIONAR']} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);
    expect(screen.queryByRole('button',{name:'Gestionar consultorios'})).not.toBeInTheDocument();unmount();
    render(<SessionView cuenta={{...base,roles:['ADM'],permisos:['CONSULTORIOS_GESTIONAR']} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);
    expect(screen.getByRole('button',{name:'Gestionar consultorios'})).toBeVisible();
  });
  it('presenta formularios accesibles de alta, edición y error',async()=>{
    setup(()=>response(409,{code:'CODIGO_DUPLICADO'}));
    const {container,rerender}=render(<main><ConsultorioForm consultorio={null} {...props()}/></main>);
    await screen.findByLabelText('Consulta general');
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
    rerender(<main><ConsultorioForm key="edicion" consultorio={consultorio} {...props()}/></main>);
    fireEvent.click(screen.getByRole('button',{name:'Guardar consultorio'}));await screen.findByRole('alert');
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
  });
});
