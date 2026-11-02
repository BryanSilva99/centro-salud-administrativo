from reportlab.lib.pagesizes import landscape
from reportlab.lib.units import inch
from reportlab.pdfgen import canvas

W, H = landscape((8.5 * inch, 5.63 * inch))

SLIDES = []

SLIDES.append({
    'title': 'AGENTS.md',
    'subtitle': 'Reglas del Asistente de IA - Centro de Salud Administrativo',
    'lines': ['Equipo: Bryan, Oscar, Percy, J.J.', 'Expo Parcial - Octubre 2026'],
    'bg': (0.0, 0.2, 0.4),
})

SLIDES.append({
    'title': 'AGENTS.md - Que es',
    'bg': (1.0, 0.98, 0.96),
    'title_color': (1.0, 0.4, 0.0),
    'lines': [
        '**AGENTS.md** es el archivo de instrucciones para el asistente de IA que trabaja en este repositorio.',
        '',
        'Su objetivo:',
        '  * Construir un sistema mantenible, coherente, legible y tecnicamente solido',
        '  * Evitar la sobreingenieria y la complejidad innecesaria',
        '',
        '**El codigo existente es una referencia de convenciones, no una garantia de que cada decision anterior sea correcta.',
    ],
})

SLIDES.append({
    'title': 'Prioridad de Decisiones',
    'bg': (1.0, 0.98, 0.98),
    'title_color': (0.8, 0.0, 0.0),
    'lines': [
        '**Prioridad: correccion > claridad > simplicidad > mantenibilidad > optimizacion prematura.',
        '',
        'La arquitectura debe facilitar la evolucion del sistema, no convertirse en una obligacion de agregar capas o abstracciones sin necesidad.',
    ],
})

SLIDES.append({
    'title': 'Control de Alcance',
    'bg': (1.0, 0.9, 0.9),
    'title_color': (0.8, 0.0, 0.0),
    'lines': [
        '**Regla fundamental:',
        '  * "Implementar unicamente lo solicitado explicitamente en la tarea actual."',
        '  * "No asumir que una funcionalidad pendiente, una fecha de entrega o una descripcion arquitectonica autorizan su implementacion."',
        '',
        'No se realizan:',
        '  * Refactorizaciones extensas',
        '  * Cambios arquitectonicos globales',
        '  * Funcionalidades no solicitadas',
    ],
})

SLIDES.append({
    'title': 'Reglas de Arquitectura para el Asistente',
    'bg': (0.94, 0.92, 0.98),
    'title_color': (0.4, 0.0, 0.6),
    'lines': [
        '**Dominio:',
        '  * "Contiene entidades, objetos de valor, invariantes y reglas de negocio."',
        '  * "No crear servicios de dominio si la logica pertenece naturalmente a una entidad."',
        '',
        '**Adaptadores:',
        '  * "Los controladores no deben contener reglas de negocio."',
        '  * "Los repositorios no deben decidir politicas de negocio."',
        '  * "No exponer entidades JPA directamente como respuestas HTTP."',
    ],
})

SLIDES.append({
    'title': 'Calidad, Pruebas y Flujo',
    'bg': (0.92, 0.95, 1.0),
    'title_color': (0.0, 0.2, 0.4),
    'lines': [
        '**Pruebas:',
        '  * "Las pruebas deben verificar comportamientos y reglas relevantes, no simplemente aumentar su cantidad."',
        '',
        '**Consistencia:',
        '  * "Despues de modificar codigo Java, ejecutar desde backend: `./mvnw spotless:check` "',
        '',
        '**Flujo obligatorio:',
        '  * "Antes de finalizar: revisar si existe una solucion mas simple, si las responsabilidades estan correctamente distribuidas, si las pruebas relevantes pasan."',
    ],
})

SLIDES.append({
    'title': 'Principio Fundamental',
    'bg': (0.96, 0.96, 1.0),
    'title_color': (0.0, 0.4, 0.8),
    'lines': [
        '**"El mejor codigo no es el que utiliza mas patrones, capas o abstracciones, sino el que resuelve correctamente el problema y puede ser comprendido, probado y modificado con el menor esfuerzo razonable."',
        '',
        '**Si dos soluciones son igualmente correctas y respetan los requisitos, preferir la que tenga menor complejidad y mayor claridad.',
    ],
})

OUT = 'C:\\Users\\Juanjo\\centro-salud-jj\\Presentacion_Expo_Agents_Centro_Salud.pdf'

from reportlab.pdfgen.canvas import Canvas

c = Canvas(OUT, pagesize=(W, H))

def draw_slide(sl):
    c.setPageSize((W, H))
    c.saveState()
    c.setFillColorRGB(*sl['bg'])
    c.rect(0, 0, W, H, fill=1, stroke=0)
    c.setFillColorRGB(0.97, 0.97, 0.97)
    c.setStrokeColorRGB(0.8, 0.8, 0.8)
    c.rect(0.5 * inch, 0.5 * inch, W - inch, H - inch, stroke=1, fill=0)

    c.setFillColorRGB(*sl.get('title_color', (0.0, 0.2, 0.4)))
    c.setFont('Helvetica-Bold', 24)
    c.drawString(0.7 * inch, H - 0.9 * inch, sl['title'])
    c.setStrokeColorRGB(0.8, 0.8, 0.8)
    c.line(0.7 * inch, H - 1.05 * inch, W - 0.7 * inch, H - 1.05 * inch)

    c.setFillColorRGB(0.08, 0.08, 0.08)
    c.setFont('Helvetica', 12)
    y = H - 1.35 * inch
    for line in sl['lines']:
        if line == '':
            y -= 8
            continue
        bold = line.startswith('**')
        text = line[2:-2] if bold else line
        c.setFont('Helvetica-Bold' if bold else 'Helvetica', 14 if bold else 12)
        c.drawString(0.7 * inch, y, text)
        y -= 16
        if y < 1.0 * inch:
            break

    if 'subtitle' in sl:
        c.setFont('Helvetica', 12)
        c.setFillColorRGB(0.6, 0.6, 0.6)
        c.drawString(0.7 * inch, 0.7 * inch, sl['subtitle'])
        c.setFillColorRGB(0.1, 0.1, 0.1)
        c.setFont('Helvetica-Bold', 12)
        c.drawString(0.7 * inch, H - 0.55 * inch, 'Equipo: Bryan, Oscar, Percy, J.J.')
        c.setFont('Helvetica', 10)
        c.setFillColorRGB(0.5, 0.5, 0.5)
        c.drawString(0.7 * inch, H - 0.78 * inch, 'Expo Parcial - Octubre 2026')
    c.restoreState()

for s in SLIDES:
    draw_slide(s)
    c.showPage()

c.save()
print('PDF creado:', OUT)
