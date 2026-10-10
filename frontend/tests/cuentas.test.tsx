import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import axe from 'axe-core';
import { CrearCuentaForm, EditarCuentaForm } from '../src/features/identity/components/FormulariosCuenta';
import { RolesPanel } from '../src/features/identity/components/RolesPanel';
import { SessionView } from '../src/features/identity/components/SessionView';
import type { Cuenta } from '../src/features/identity/api/auth';
import type { CuentaRoles } from '../src/features/identity/api/roles';
const cuenta: CuentaRoles = { id: 'cuenta-prueba', identificador: 'recepcion', activa: true, roles: ['REC'], version: 4 };
const response=(status: number,body: unknown)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
function setup(write: (url: string, options: RequestInit) => Response) {
  const fetch=vi.fn(async(url: string,options: RequestInit={})=> {
    if(url.endsWith('/csrf'))return response(200,{headerName:'X-CSRF-TOKEN',token:'csrf'});
    if(options.method==='POST'||options.method==='PUT')return write(url,options);
    if(url.endsWith('/me'))return response(200,{...cuenta,roles:['ADM'],permisos:['CUENTAS_GESTIONAR','ROLES_GESTIONAR']});
    if(url.includes('/roles/auditoria'))return response(200,{elementos:[],hayMas:false});
    if(url.includes('/auditoria'))return response(200,{elementos:[{id:'audit',actorId:'admin',tipo:'CREACION',identificadorAnterior:null,identificadorNuevo:'recepcion',activaAnterior:null,activaNueva:true,roles:['REC'],fecha:'2026-10-10T10:00:00Z'}],hayMas:false});
    if(url.includes('?busqueda='))return response(200,{elementos:[cuenta],hayMas:false});
    return response(200,cuenta);
  });vi.stubGlobal('fetch',fetch);return fetch;
}
function props(){return {busy:false,onBusy:vi.fn(),onSaved:vi.fn(async()=>{}),onExpired:vi.fn()};}
function fill(clave='contraseña-de-prueba') {
  fireEvent.change(screen.getByLabelText('Identificador de la nueva cuenta'),{target:{value:'nueva.cuenta'}});
  fireEvent.change(screen.getByLabelText('Contraseña inicial',{exact:true}),{target:{value:clave}});
  fireEvent.change(screen.getByLabelText('Confirmar contraseña inicial'),{target:{value:clave}});
}
describe('Gestión de cuentas',()=>{
  it('crea con roles y CSRF sin almacenar la contraseña, y limpia los campos secretos',async()=>{
    const save=vi.fn((url: string,options: RequestInit)=>{
      expect(url).toBe('/api/cuentas');expect(options.headers).toMatchObject({'X-CSRF-TOKEN':'csrf'});
      expect(JSON.parse(options.body as string)).toEqual({identificador:'nueva.cuenta',contrasena:'contraseña-de-prueba',roles:['CAJ']});
      return response(201,{...cuenta,identificador:'nueva.cuenta',roles:['CAJ']});
    });setup(save);const p=props();const storage=vi.spyOn(Storage.prototype,'setItem');
    render(<CrearCuentaForm {...p}/>);fill();fireEvent.click(screen.getByLabelText('Caja'));
    fireEvent.click(screen.getByRole('button',{name:'Crear cuenta'}));
    await waitFor(()=>expect(p.onSaved).toHaveBeenCalled());expect(save).toHaveBeenCalledTimes(1);
    expect(screen.getByLabelText('Contraseña inicial',{exact:true})).toHaveValue('');expect(screen.getByLabelText('Confirmar contraseña inicial')).toHaveValue('');expect(storage).not.toHaveBeenCalled();
  });
  it('rechaza claves cortas o confirmaciones distintas antes de enviar',async()=>{
    const fetch=setup(()=>response(201,cuenta));render(<CrearCuentaForm {...props()}/>);fill('corta');
    fireEvent.click(screen.getByRole('button',{name:'Crear cuenta'}));expect(await screen.findByRole('alert')).toHaveTextContent('12 y 128');expect(fetch).not.toHaveBeenCalled();
    fill();fireEvent.change(screen.getByLabelText('Confirmar contraseña inicial'),{target:{value:'otra-contraseña'}});
    fireEvent.click(screen.getByRole('button',{name:'Crear cuenta'}));expect(await screen.findByRole('alert')).toHaveTextContent('no coinciden');expect(fetch).not.toHaveBeenCalled();
  });
  it('muestra duplicados y no repite automáticamente el alta',async()=>{
    const save=vi.fn(()=>response(409,{code:'DUPLICADO'}));setup(save);render(<CrearCuentaForm {...props()}/>);fill();
    fireEvent.click(screen.getByRole('button',{name:'Crear cuenta'}));expect(await screen.findByRole('alert')).toHaveTextContent('ya está en uso');expect(save).toHaveBeenCalledTimes(1);
    expect(screen.getByLabelText('Identificador de la nueva cuenta')).toHaveValue('nueva.cuenta');expect(screen.getByLabelText('Contraseña inicial',{exact:true})).toHaveValue('');
  });
  it('desactiva enviando la versión y avisa de la revocación de sesiones',async()=>{
    setup((url,options)=>{expect(url).toBe('/api/cuentas/cuenta-prueba');expect(JSON.parse(options.body as string)).toEqual({identificador:'recepcion',activa:false,version:4});return response(200,{...cuenta,activa:false,version:5});});
    const p=props();render(<EditarCuentaForm {...p} cuenta={cuenta}/>);fireEvent.click(screen.getByLabelText('Cuenta activa'));
    expect(screen.getByText(/sesiones abiertas dejarán/)).toBeVisible();fireEvent.click(screen.getByRole('button',{name:'Guardar cuenta'}));await waitFor(()=>expect(p.onSaved).toHaveBeenCalledWith({...cuenta,activa:false,version:5}));
  });
  it('bloquea ediciones obsoletas y explica la protección del último administrador',async()=>{
    setup(()=>response(409,{code:'CONFLICTO'}));const {unmount}=render(<EditarCuentaForm {...props()} cuenta={cuenta}/>);
    fireEvent.click(screen.getByLabelText('Cuenta activa'));fireEvent.click(screen.getByRole('button',{name:'Guardar cuenta'}));
    expect(await screen.findByRole('alert')).toHaveTextContent('Recarga');expect(screen.getByRole('button',{name:'Guardar cuenta'})).toBeDisabled();unmount();
    setup(()=>response(409,{code:'ULTIMO_ADMIN'}));render(<EditarCuentaForm {...props()} cuenta={cuenta}/>);
    fireEvent.click(screen.getByLabelText('Cuenta activa'));fireEvent.click(screen.getByRole('button',{name:'Guardar cuenta'}));expect(await screen.findByRole('alert')).toHaveTextContent('administrador activo');
  });
  it('muestra la auditoría de cuenta y no ofrece gestión a quien no tiene permisos',async()=>{
    setup(()=>response(200,cuenta));const {unmount}=render(<RolesPanel gestionarCuentas onRefresh={vi.fn()} onExpired={vi.fn()}/>);
    fireEvent.click(await screen.findByRole('button',{name:'recepcion · Activa'}));expect(await screen.findByText('Creación: recepcion')).toBeVisible();expect(screen.getByRole('button',{name:'Guardar cuenta'})).toBeDisabled();unmount();
    render(<SessionView cuenta={{...cuenta,permisos:[]} as Cuenta} onRefresh={vi.fn()} onExit={vi.fn()}/>);expect(screen.queryByRole('button',{name:'Administrar cuentas y roles'})).not.toBeInTheDocument();
  });
  it('presenta formularios accesibles de alta y edición',async()=>{
    setup(()=>response(200,cuenta));const {container,rerender}=render(<main><CrearCuentaForm {...props()}/></main>);
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
    rerender(<main><EditarCuentaForm {...props()} cuenta={cuenta}/></main>);
    expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
  });
});
