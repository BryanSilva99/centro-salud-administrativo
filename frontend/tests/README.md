# Pruebas del login

`npm test` ejecuta Vitest, Testing Library y Axe-core en jsdom. Se comprueban login/logout, restauración de sesión, errores, CSRF, actividad y ausencia de persistencia de secretos en almacenamiento del navegador.

Axe analiza formulario y error con contraste desactivado por la ausencia de layout real en jsdom. Complementar con revisión visual y teclado; no se atribuye accesibilidad completa a este resultado.
