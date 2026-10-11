import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import axe from 'axe-core';
import { RegistrarPacienteForm } from '../src/features/administration/components/RegistrarPacienteForm';
import { SessionView } from '../src/features/identity/components/SessionView';
import type { Cuenta } from '../src/features/identity/api/auth';
const response=(status: number,body: unknown)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
const registrado={id:'paciente',codigo:'P00000042',tipoDocumento:'CE',numeroDocumento:'AB1234567',nombres:'Ana',apellidos:'Quispe',fechaNacimiento:'1990-05-20',telefono:null,correo:null};
function setup(write: (options: RequestInit) => Response | Promise<Response>) {
  const fetch=vi.fn(async(url: string,options: RequestInit={})=>{
    if(url.endsWith('/csrf'))return response(200,{headerName:'X-CSRF-TOKEN',token:'csrf'});
    if(options.method==='POST'){expect(url).toBe('/api/pacientes');return write(options);}
    return response(200,{});
  });vi.stubGlobal('fetch',fetch);return fetch;
}
function fill() {
  fireEvent.change(screen.getByLabelText('Tipo de documento'),{target:{value:'CE'}});
  fireEvent.change(screen.getByLabelText('Número de documento'),{target:{value:'ab1234567'}});
  fireEvent.change(screen.getByLabelText('Nombres'),{target:{value:' Ana '}});
  fireEvent.change(screen.getByLabelText('Apellidos'),{target:{value:'Quispe'}});
  fireEvent.change(screen.getByLabelText('Fecha de nacimiento'),{target:{value:'1990-05-20'}});
}
describe('Registro de pacientes',()=>{
  it('envía los datos con CSRF y muestra el código asignado',async()=>{
    const save=vi.fn((options: RequestInit)=>{
      expect(options.headers).toMatchObject({'X-CSRF-TOKEN':'csrf'});
      expect(JSON.parse(options.body as string)).toEqual({tipoDocumento:'CE',numeroDocumento:'ab1234567',nombres:'Ana',apellidos:'Quispe',fechaNacimiento:'1990-05-20',telefono:null,correo:null});
      return response(201,registrado);
    });setup(save);
    render(<RegistrarPacienteForm onExpired={vi.fn()}/>);fill();
    fireEvent.click(screen.getByRole('button',{name:'Registrar paciente'}));
    expect(await screen.findByRole('status')).toHaveTextContent('P00000042');
    expect(save).toHaveBeenCalledTimes(1);expect(screen.getByLabelText('Nombres')).toHaveValue('');
  });
  it('explica el duplicado sin repetir el envío',async()=>{
    const save=vi.fn(()=>response(409,{code:'DOCUMENTO_DUPLICADO'}));setup(save);
    render(<RegistrarPacienteForm onExpired={vi.fn()}/>);fill();
    fireEvent.click(screen.getByRole('button',{name:'Registrar paciente'}));
    expect(await screen.findByRole('alert')).toHaveTextContent('registro existente');
    expect(save).toHaveBeenCalledTimes(1);expect(screen.getByLabelText('Nombres')).toHaveValue(' Ana ');
  });
  it('ante un fallo de red advierte comprobar antes de repetir y no reintenta',async()=>{
    const save=vi.fn(()=>{throw new TypeError('network');});setup(save);
    render(<RegistrarPacienteForm onExpired={vi.fn()}/>);fill();
    fireEvent.click(screen.getByRole('button',{name:'Registrar paciente'}));
    expect(await screen.findByRole('alert')).toHaveTextContent('Antes de repetir el registro');expect(save).toHaveBeenCalledTimes(1);
  });
  it('ignora un segundo envío mientras el primero está en curso',async()=>{
    let terminar: (r: Response)=>void=()=>{};
    const save=vi.fn(()=>new Promise<Response>(resolve=>{terminar=resolve;}));setup(save);
    render(<RegistrarPacienteForm onExpired={vi.fn()}/>);fill();
    const form=screen.getByRole('form',{name:'Registrar paciente'});
    fireEvent.submit(form);fireEvent.submit(form);
    await waitFor(()=>expect(save).toHaveBeenCalledTimes(1));
    expect(screen.getByRole('button',{name:'Registrando paciente…'})).toBeDisabled();
    terminar(response(201,registrado));
    expect(await screen.findByRole('status')).toHaveTextContent('P00000042');expect(save).toHaveBeenCalledTimes(1);
  });
  it('una sesión vencida vuelve al inicio de sesión',async()=>{
    setup(()=>response(401,{}));const onExpired=vi.fn();
    render(<RegistrarPacienteForm onExpired={onExpired}/>);fill();
    fireEvent.click(screen.getByRole('button',{name:'Registrar paciente'}));
    await waitFor(()=>expect(onExpired).toHaveBeenCalled());
  });
  it('solo ofrece el registro a cuentas con permiso',()=>{
    setup(()=>response(201,registrado));
    const base={id:'c',identificador:'recepcion',activa:true,roles:['REC']} as const;
    const {unmount}=render(<SessionView cuenta={{...base,roles:['ADM'],permisos:['CUENTAS_GESTIONAR']} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);
    expect(screen.queryByRole('button',{name:'Registrar paciente'})).not.toBeInTheDocument();unmount();
    render(<SessionView cuenta={{...base,roles:['REC'],permisos:['PACIENTES_GESTIONAR']} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);
    fireEvent.click(screen.getByRole('button',{name:'Registrar paciente'}));
    expect(screen.getByRole('form',{name:'Registrar paciente'})).toBeVisible();
  });
  it('presenta un formulario accesible en estado inicial y de error',async()=>{
    setup(()=>response(409,{code:'DOCUMENTO_DUPLICADO'}));
    const {container}=render(<main><RegistrarPacienteForm onExpired={vi.fn()}/></main>);
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
    fill();fireEvent.click(screen.getByRole('button',{name:'Registrar paciente'}));await screen.findByRole('alert');
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
  });
});
