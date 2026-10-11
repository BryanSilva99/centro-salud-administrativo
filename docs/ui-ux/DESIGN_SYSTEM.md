# Sistema visual: especificación y procedencia

Fecha de inspección: 11 de octubre de 2026. Base común: `main`, commit `d7aca93`. Referencia adicional revisada: `feature/pacientes`, commit `fe559f3`, pendiente de integrar en main.

Esta documentación es transversal al proyecto. Las referencias a código de pacientes describen la rama revisada, no funcionalidad ya disponible en main.

## Autoridad y alcance

Figma define la apariencia y composición; los [requisitos](../02_REQUERIMIENTOS_DEL_SISTEMA.md) y contratos actuales definen campos, permisos y operaciones. Por instrucción del responsable, una diferencia del mockup no autoriza ampliar el backend ni inventar comportamiento. Las limitaciones funcionales se recogen en [PATRONES_UX](PATRONES_UX.md).

Esta especificación registra propiedades verificadas mediante Figma MCP; no afirma conformidad visual del frontend. **F** = propiedad extraída de Figma; **C** = código actual. La extracción antes bloqueada se completó el 11 de octubre de 2026. Las muestras tienen nombres de tokens, pero el archivo devuelve cero variables locales y cero estilos locales de pintura, texto y efectos. Los JSON de [evidencias](FIGMA_REFERENCIAS.md#evidencia-local-de-la-extracción) conservan los valores y nodos consultados.

## Colores

| Nombre Figma | Nodo | Valor objetivo |
|---|---|---|
| Primary | 23:12 | `#2d3a5e`; opacidad 1 |
| Accent | 23:14 | `#8fa9ff`; opacidad 1 |
| Surface | 23:16 | `#fffdfb`; opacidad 1 |
| Background | 23:18 | `#fffdfb`; opacidad 1 |
| Success | 23:20 | `#15803d`; opacidad 1 |
| Warning | 23:22 | `#b45309`; opacidad 1 |
| Danger | 23:24 | `#b91c1c`; opacidad 1 |
| Info | 23:26 | `#0369a1`; opacidad 1 |

Las ocho muestras son F y miden 132 × 62, separadas 16 px horizontalmente en el frame 23:9. Esa separación es una medida local, no una escala global de espaciado.

Inventario C de `frontend/src/app/styles.css`, **no valores extraídos de Figma ni sustitutos autorizados**:

| Uso actual | Selector / valor |
|---|---|
| Fondo y texto base | `:root`: `#f5f7f6`, `#193735` |
| Superficie | `.main-layout`: blanco |
| Acción principal | `.primary-button`: `#176454`; hover `#104e41`; texto blanco |
| Panel de bienvenida | `.welcome-panel`: `#e7efeb` |
| Campo | `input`: borde `#bacbc4`, radio 8 px |
| Error | `.error`: fondo `#fff0ed`, texto `#8e3025`, borde `#e9bfb7` |
| Aviso | `.notice`: fondo `#edf5ef`, texto `#355f50`, borde `#d5e7dc` |
| Foco | `:focus-visible`: 3 px `#a16116`, separación 4 px |

Los sistemas no coinciden: la aplicación actual es verde; Figma utiliza azul oscuro, acento azul y superficies cálidas. La adaptación futura debe usar los valores F, sin cambiar comportamiento.

## Tipografía

F: el texto de fundamentos 23:28–23:29 especifica **Inter**, títulos **26 / Semi Bold**, sección **15 / Semi Bold**, cuerpo **13 / Regular**. La inspección de los 2012 nodos TEXT confirma Inter, interlineado AUTO y tracking 0 %. Los tamaños y pesos varían por función: títulos de pantalla 26/600 (23:111), login 30/600 (23:50), secciones 15/600 (23:78), texto 13/400 (23:11), botones iniciales 13/600 (23:4), etiquetas 12/500 (23:52) y badges 11/500 (23:37). Hay tamaños adicionales de 9 a 31 px y pesos 400, 500, 600 y 700: el inventario registra cada combinación, frecuencia y nodo de ejemplo. No reducirlos a tres tamaños ni convertir AUTO en un interlineado numérico inventado. La altura de la caja no es el tamaño de fuente.

C: `:root` usa `Inter, "Segoe UI", system-ui, sans-serif`, peso 400 e interlineado 1.5. `h1` usa 30 px, line-height 1.2 y tracking -.035em; etiquetas 13 px/650; CTA 14 px/650. La lista de fallback no garantiza que Inter esté cargada. La adaptación debe comprobar la fuente realmente renderizada y disponibilidad/licencia; no añadir dependencias en esta tarea.

## Medidas y estructura verificadas

| Elemento | Medida F | Nodo / alcance |
|---|---|---|
| Pantallas desktop | 1360 × 860 | 43 frames de pantalla/patrones |
| Sistema visual | 1360 × 420 | 23:9 |
| Login: panel de acceso | 660 × 860, x=0 | 23:43 |
| Login: panel de marca | 700 × 860, x=660 | 23:44 |
| Campo login | 470 × 42 | 23:53, 23:56 |
| CTA login | 470 × 40 | 23:59 |
| Sidebar inicial | 248 × 860 | 23:73, Inicio REC |
| Topbar inicial | 1112 × 64, x=248 | 23:102 |
| Contenido inicial | título x=280 | 23:111; margen local de 32 respecto a sidebar |
| Navegación activa | 224 × 42, x=12 | 23:82 |
| Separación de entradas de navegación | 48 px entre posiciones iniciales | 23:83/86/89; no altura universal |
| Icon tile inicial | 22 × 22 | 23:83 |
| Cabecera de tabla inicial | 618 × 40 | 23:142 |
| Fila alterna inicial | 618 × 46 | 23:153 |
| Badge inicial | alto 26; ancho según texto | 23:36/38/40 |
| Variante tardía de estructura | sidebar hasta x=232; topbar 1128 × 64 | 28:837, frame 28:816 |
| Variante tardía: contenido | x=260 | 28:842; margen local 28 |
| Variante tardía: campo | 300 × 40 | 28:850 |
| Variante tardía: botones | 175 × 38 y 120 × 38 | 28:863/865 |

**Hay variantes entre frames:** sidebar 248/232, botones de 40/38. No homogeneizarlas silenciosamente. Conservar la referencia de cada pantalla y mantener ambas estructuras como variantes explícitas; una unificación posterior requiere una decisión visual distinta de esta extracción. Las posiciones sobre el canvas no son rutas ni orden de navegación.

C: login actual tiene presentación a la izquierda y formulario a la derecha; Figma coloca acceso a la izquierda y marca a la derecha. El frontend usa contenedor máximo 1440, panel principal redondeado de 24, puntos responsive 900 y 680; al abrir `.roles-panel`, cambia a una columna y máximo 960. **Esos breakpoints y radios no se verificaron en Figma.** No se deducen diseños móvil/tablet de los frames desktop.

## Componentes y recursos

| Referencia | Evidencia F | Correspondencia C / pendiente |
|---|---|---|
| Button / Primary, Secondary, Danger | Componentes 23:3/5/7, 144 × 40; instancias de ejemplo 23:30/32/34, 148 × 40 | Clases CSS compartidas; no biblioteca de botones React. Radio 10, auto-layout horizontal centrado, padding 0 y gap 8. Primary/Danger sin borde; Secondary con borde 1 px #e8e3f0. No hay component sets ni variantes hover/foco/disabled |
| Inputs y grupos | Campos etiquetados y grupos administrativos en 24:22 | `RegistrarPacienteForm`, inputs/select/fieldset nativos. Fondo #fffdfb, borde 1 px #e8e3f0, radio 8; medidas por formulario en la tabla inferior |
| Sidebar/topbar | Estructuras repetidas por rol | No existen en `App.tsx`; propuesta visual futura, sin rutas adicionales definidas |
| Tabla y paginación | Pacientes 23:497; cuentas 28:412 | `RolesPanel` utiliza listas y paginación; directorio de pacientes no implementado |
| Badges y avisos | Estados 23:36–40 y frame 28:867 | `.status-badge`, `.error`, `.notice`; Figma: badges radio 13 y altura 26; equivalencia con CSS actual no implementada |
| Marca e ilustración | Figuras de cruz y paneles en login 23:45–70 | Marca actual `+` y figuras CSS; Figma utiliza rectángulos redondeados; no una imagen o biblioteca externa. Ver componentes.json, nodos 23:45–47 |
| Iconografía | Textos/abreviaturas visibles en metadatos: IN, PA, AG, AD, HI, ⌕, ▢ | El inventario de la página contiene solo FRAME, TEXT, RECTANGLE, INSTANCE y COMPONENT; no hay vectores ni rellenos de imagen. Son textos y geometría, no iconos de Lucide/Material. No se recuperan archivos ni licencias de iconos porque no hay recursos externos de ese tipo en la página |

## Puerta de verificación visual

Antes de modificar una vista: consultar las propiedades verificadas y el frame individual vigente; registrar cualquier cambio posterior del diseño. Conservar paleta y estructura originales salvo la exclusión funcional documentada. Comparar React a 1360 × 860 y verificar adicionalmente 1366 × 768 por RNF-08. No declarar fidelidad sin comparación de captura, fuente, geometría, recursos y estados. No utilizar una captura del mockup como fondo para simular una implementación.

Cobertura, evidencia y elementos no definidos por el diseño: [FIGMA_REFERENCIAS](FIGMA_REFERENCIAS.md).

## Colores complementarios y efectos

Todos los rellenos inventariados son sólidos, con opacidad de pintura 1. No hay efectos de sombra o desenfoque en la página inspeccionada. No añadirlos como si procedieran de Figma.

| Uso / muestra | Valor F | Nodo |
|---|---|---|
| Texto secundario | #475569 | 23:11 |
| Ayuda | #64748b | 23:58 |
| Marca y selección en sidebar clara | #fbe7f1 | 23:44, 23:82 |
| Bordes / divisores | #e8e3f0 | 23:53, 24:30 |
| Badges iniciales éxito / espera / información | #f0fdf4 / #fffbeb / #f0f9ff | 23:36/38/40 |
| Acento violeta tardío | #d7c6ff | 28:536 |
| Estados carga / éxito / error | #dbeafe / #dcfce7 / #fee2e2 | 28:880/885/890 |
| Advertencia del frame de patrones | #d7c6ff | 28:895; no sustituir por amarillo automáticamente |
| Otros fondos existentes | #fef2f2 / #fef3c7 | 24:703 / 28:302 |

Surface y Background tienen exactamente el mismo valor en Figma; no se trata de una omisión. Los nombres semánticos no autorizan normalizar diferencias entre badges de distintas pantallas.

## Geometría de formularios y estados

| Elemento | Medidas y separación verificadas | Fuente |
|---|---|---|
| Tarjeta de pacientes | x280, y180, 1046×520, radio12, borde1 | 24:65; edición 24:142 |
| Campos de pacientes | 460×42, x304 y794: separación30; y297/385/473/561: paso88 | 24:70–91; edición 24:147–168 |
| Acciones de pacientes | Cancelar144×40 y Crear168×40 en y632; radio10 | 24:95/97 |
| Tarjeta de cuenta | x260,y170,700×420,radio12,borde1 | 28:518 |
| Campos de cuenta | 300×40; x20 y350 respecto a tarjeta: separación30; paso vertical88 | 28:522/525/528/531/534 |
| Acciones de cuenta | Guardar175×38, Cancelar120×38; radio8 | 28:538/540 |
| Tarjetas de estados | 590×165, radio12, borde1; x40/680, y130/335/540 | 28:870/877/882/887/892 |

Son medidas locales: no existe una escala de espaciado publicada. No confundir coordenadas relativas a la tarjeta con coordenadas del frame. En pacientes la tarjeta comienza en x280 y los campos en x304 (24 px); en cuentas el campo tiene x20 relativo a la tarjeta. La página utiliza 305 frames sin auto-layout y 69 componentes/instancias con disposición horizontal; las 3306 constraints consultadas son MIN/MIN. Esto no define reglas responsive. Los radios existentes abarcan 0,2,4,6,8,9,10,11,12,13,14,16,17,18,24 y32: se conserva cada uso, no una escala inventada.

## Variantes visuales y recursos ausentes

La familia inicial (por ejemplo pacientes 24:22) tiene sidebar claro de 248 px, selección rosa y abreviaturas como iconos. La familia tardía (cuentas 28:488) tiene sidebar azul oscuro de 232 px, selección azul #8fa9ff y texto claro. Ambas aparecen en los renders inspeccionados y deben conservarse por pantalla. No atribuir el cambio solamente al rol: existen pantallas ADM en ambas familias.

No se encontraron diseños móvil/tablet dentro de los 44 frames de Mockups, variantes de hover/foco/disabled, ni reacciones de prototipo. El frame 28:867 sí define estados estáticos vacío/carga/éxito/error/advertencia. Son ausencias del diseño, no consultas bloqueadas. Las reglas de teclado, seguridad y validación siguen proviniendo de requisitos y código. La API expone la familia Inter, pero no entrega un archivo de fuente ni su licencia; obtenerlos de su distribución legítima al implementar no forma parte de la extracción visual.
