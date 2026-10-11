import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import axe from 'axe-core';
import { EditarPacienteForm } from '../src/features/administration/components/EditarPacienteForm';
import { BuscarPacientesPanel } from '../src/features/administration/components/BuscarPacientesPanel';
import { SessionView } from '../src/features/identity/components/SessionView';
import type { Paciente } from '../src/features/administration/api/pacientes';
import type { Cuenta } from '../src/features/identity/api/auth';
const response=(status: number,body: unknown)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
const paciente: Paciente={id:'p1',codigo:'P00000001',tipoDocumento:'DNI',numeroDocumento:'12345678',nombres:'Ana',apellidos:'Quispe',fechaNacimiento:'1990-05-20',telefono:null,correo:null,version:2};
function setup(write: (url: string, options: RequestInit) => Response, pagina: {elementos: Paciente[]; hayMas: boolean}={elementos:[paciente],hayMas:false}) {
  const fetch=vi.fn(async(url: string,options: RequestInit={})=>{
    if(url.endsWith('/csrf'))return response(200,{headerName:'X-CSRF-TOKEN',token:'csrf'});
    if(options.method==='PUT')return write(url,options);
    if(url.startsWith('/api/pacientes?'))return response(200,pagina);
    return response(404,{});
  });vi.stubGlobal('fetch',fetch);return fetch;
}
const props=()=>({onSaved:vi.fn(),onReload:vi.fn(),onExpired:vi.fn()});
describe('Edición y búsqueda de pacientes',()=>{
  it('actualiza conservando el código y enviando la versión leída',async()=>{
    const save=vi.fn((url: string,options: RequestInit)=>{
      expect(url).toBe('/api/pacientes/p1');expect(options.headers).toMatchObject({'X-CSRF-TOKEN':'csrf'});
      expect(JSON.parse(options.body as string)).toEqual({tipoDocumento:'CE',numeroDocumento:'ab1234567',nombres:'Ana María',apellidos:'Quispe',fechaNacimiento:'1990-05-20',telefono:null,correo:null,version:2});
      return response(200,{...paciente,tipoDocumento:'CE',numeroDocumento:'ab1234567',nombres:'Ana María',version:3});
    });setup(save);const p=props();
    render(<EditarPacienteForm paciente={paciente} {...p}/>);
    fireEvent.change(screen.getByLabelText('Tipo de documento'),{target:{value:'CE'}});
    fireEvent.change(screen.getByLabelText('Número de documento'),{target:{value:'ab1234567'}});
    fireEvent.change(screen.getByLabelText('Nombres'),{target:{value:'Ana María'}});
    fireEvent.click(screen.getByRole('button',{name:'Guardar paciente'}));
    await waitFor(()=>expect(p.onSaved).toHaveBeenCalledWith({...paciente,tipoDocumento:'CE',numeroDocumento:'ab1234567',nombres:'Ana María',version:3}));
    expect(save).toHaveBeenCalledTimes(1);
  });
  it('ante una edición obsoleta bloquea el guardado y ofrece recargar',async()=>{
    const save=vi.fn(()=>response(409,{code:'CONFLICTO'}));setup(save);const p=props();
    render(<EditarPacienteForm paciente={paciente} {...p}/>);
    fireEvent.click(screen.getByRole('button',{name:'Guardar paciente'}));
    expect(await screen.findByRole('alert')).toHaveTextContent('Recárgala');
    expect(screen.getByRole('button',{name:'Guardar paciente'})).toBeDisabled();
    fireEvent.click(screen.getByRole('button',{name:'Recargar paciente'}));expect(p.onReload).toHaveBeenCalled();expect(save).toHaveBeenCalledTimes(1);
  });
  it('explica un documento duplicado',async()=>{
    setup(()=>response(409,{code:'DOCUMENTO_DUPLICADO'}));
    render(<EditarPacienteForm paciente={paciente} {...props()}/>);
    fireEvent.click(screen.getByRole('button',{name:'Guardar paciente'}));
    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe otro paciente');
  });
  it('lista los resultados de la búsqueda y abre el seleccionado para editar',async()=>{
    setup(()=>response(200,paciente),{elementos:[paciente,{...paciente,id:'p2',codigo:'P00000002',nombres:'Luis'}],hayMas:true});
    render(<BuscarPacientesPanel onExpired={vi.fn()}/>);
    fireEvent.change(screen.getByLabelText('Documento, nombre o código'),{target:{value:'Quispe'}});
    fireEvent.click(screen.getByRole('button',{name:'Buscar'}));
    expect(await screen.findByRole('button',{name:'P00000002 · Luis Quispe'})).toBeVisible();
    fireEvent.click(screen.getByRole('button',{name:'P00000001 · Ana Quispe'}));
    expect(screen.getByRole('form',{name:'Editar P00000001'})).toBeVisible();
    expect(screen.getByRole('button',{name:'Página siguiente'})).toBeEnabled();
  });
  it('solo ofrece la búsqueda a cuentas con permiso',()=>{
    setup(()=>response(200,paciente));
    const base={id:'c',identificador:'cuenta',activa:true} as const;
    const {unmount}=render(<SessionView cuenta={{...base,roles:['ADM'],permisos:['CONSULTORIOS_GESTIONAR']} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);
    expect(screen.queryByRole('button',{name:'Buscar pacientes'})).not.toBeInTheDocument();unmount();
    render(<SessionView cuenta={{...base,roles:['REC'],permisos:['PACIENTES_GESTIONAR']} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);
    fireEvent.click(screen.getByRole('button',{name:'Buscar pacientes'}));
    expect(screen.getByLabelText('Documento, nombre o código')).toBeVisible();
  });
  it('presenta un formulario de edición accesible en estado inicial y de error',async()=>{
    setup(()=>response(409,{code:'CONFLICTO'}));
    const {container}=render(<main><EditarPacienteForm paciente={paciente} {...props()}/></main>);
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
    fireEvent.click(screen.getByRole('button',{name:'Guardar paciente'}));await screen.findByRole('alert');
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
  });
});
