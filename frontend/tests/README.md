# Pruebas del login y appointments

`npm test` ejecuta Vitest, Testing Library y Axe-core en jsdom. Se comprueban login/logout, restauración de sesión, errores, CSRF, actividad, agenda, programación, reprogramación, sala de espera, transición de atención y dashboard.

Axe analiza formulario, error y workspace con contraste desactivado por la ausencia de layout real en jsdom. Complementar con revisión visual y teclado; no se atribuye accesibilidad completa a este resultado.
