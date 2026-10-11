import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { expect, it, vi } from 'vitest';
import axe from 'axe-core';
import { VinculoProfesional } from '../src/features/identity/components/VinculoProfesional';
import type { CuentaRoles } from '../src/features/identity/api/roles';
const cuenta: CuentaRoles = { id:'cuenta',identificador:'profesional',activa:true,roles:['PRO'],version:3,profesionalId:null };
const ficha = { id:'ficha',identificacion:'P-01',nombre:'Profesional de prueba',funcion:'Medicina',activa:true };
const response=(status:number,body:unknown)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
function setup(write:(options:RequestInit)=>Response) {
  vi.stubGlobal('fetch',vi.fn(async(url:string,options:RequestInit={})=>{
    if(url.endsWith('/csrf'))return response(200,{headerName:'X-CSRF-TOKEN',token:'csrf'});
    if(options.method==='PUT'||options.method==='POST')return write(options);
    return response(200,[ficha]);
  }));
}
const props=()=>({cuenta,busy:false,onBusy:vi.fn(),onSaved:vi.fn(async()=>{}),onExpired:vi.fn()});
it('envía ficha, versión y CSRF para vincular',async()=>{
  setup(options=>{expect(JSON.parse(options.body as string)).toEqual({profesionalId:'ficha',version:3});expect(options.headers).toMatchObject({'X-CSRF-TOKEN':'csrf'});return response(200,{...cuenta,profesionalId:'ficha',version:4});});
  const p=props();render(<VinculoProfesional {...p}/>);
  await screen.findByRole('option',{name:'Profesional de prueba · P-01'});
  fireEvent.change(screen.getByLabelText('Ficha vinculada'),{target:{value:'ficha'}});
  fireEvent.click(screen.getByRole('button',{name:'Guardar vínculo'}));
  await waitFor(()=>expect(p.onSaved).toHaveBeenCalledWith({...cuenta,profesionalId:'ficha',version:4}));
});
it('explica una ficha ocupada y bloquea una edición obsoleta',async()=>{
  let code='VINCULO_OCUPADO';setup(()=>response(409,{code}));render(<VinculoProfesional {...props()}/>);
  await screen.findByRole('option',{name:'Profesional de prueba · P-01'});
  fireEvent.change(screen.getByLabelText('Ficha vinculada'),{target:{value:'ficha'}});
  fireEvent.click(screen.getByRole('button',{name:'Guardar vínculo'}));
  expect(await screen.findByRole('alert')).toHaveTextContent('otra cuenta');code='CONFLICTO';
  fireEvent.click(screen.getByRole('button',{name:'Guardar vínculo'}));
  await waitFor(()=>expect(screen.getByRole('alert')).toHaveTextContent('Recarga'));
  expect(screen.getByRole('button',{name:'Guardar vínculo'})).toBeDisabled();
});
it('registra una ficha sin vincularla silenciosamente y mantiene campos accesibles',async()=>{
  const save=vi.fn(()=>response(201,ficha));setup(save);const p=props();const {container}=render(<main><VinculoProfesional {...p}/></main>);
  fireEvent.click(screen.getByRole('button',{name:'Registrar ficha básica'}));
  fireEvent.change(screen.getByLabelText('Identificación profesional'),{target:{value:'P-01'}});
  fireEvent.change(screen.getByLabelText('Nombre completo'),{target:{value:'Profesional de prueba'}});
  fireEvent.change(screen.getByLabelText('Especialidad o función'),{target:{value:'Medicina'}});
  expect((await axe.run(container,{rules:{'color-contrast':{enabled:false}}})).violations).toEqual([]);
  fireEvent.click(screen.getByRole('button',{name:'Guardar ficha'}));
  await waitFor(()=>expect(screen.getByLabelText('Ficha vinculada')).toHaveValue('ficha'));
  expect(p.onSaved).not.toHaveBeenCalled();expect(save).toHaveBeenCalledTimes(1);
  expect(screen.getByRole('button',{name:'Guardar vínculo'})).toBeEnabled();
});
