from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from pptx.enum.shapes import MSO_SHAPE

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)

AZUL_OSCURO = RGBColor(0, 51, 102)
AZUL_MEDIO = RGBColor(0, 102, 204)
AZUL_CLARO = RGBColor(0, 153, 255)
VERDE = RGBColor(0, 153, 76)
NARANJA = RGBColor(255, 102, 0)
ROJO = RGBColor(204, 0, 0)
MORADO = RGBColor(102, 0, 153)
ROS = RGBColor(255, 0, 102)
AMARILLO = RGBColor(255, 204, 0)
BLANCO = RGBColor(255, 255, 255)
NEGRO = RGBColor(30, 30, 30)

FONDO_AZUL = RGBColor(230, 242, 255)
FONDO_VERDE = RGBColor(230, 255, 230)
FONDO_NARANJA = RGBColor(255, 240, 230)
FONDO_MORADO = RGBColor(240, 230, 255)
FONDO_ROJO = RGBColor(255, 230, 230)
FONDO_AREAL = RGBColor(245, 245, 255)

def add_background(slide, color):
    background = slide.background
    fill = background.fill
    fill.solid()
    fill.fore_color.rgb = color

def add_decorative_shape(slide, left, top, width, height, color):
    shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
    shape.fill.solid()
    shape.fill.fore_color.rgb = color
    shape.line.fill.background()
    return shape

def add_slide(title, content, bg_color=FONDO_AZUL, title_color=AZUL_OSCURO, accent_color=AZUL_MEDIO):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_background(slide, bg_color)
    add_decorative_shape(slide, Inches(0), Inches(0), Inches(13.333), Inches(0.3), accent_color)
    add_decorative_shape(slide, Inches(0), Inches(0.3), Inches(0.2), Inches(7.2), accent_color)

    title_bg = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.5), Inches(0.5), Inches(12.333), Inches(0.8))
    title_bg.fill.solid()
    title_bg.fill.fore_color.rgb = title_color
    title_bg.line.fill.background()

    title_box = slide.shapes.add_textbox(Inches(0.7), Inches(0.55), Inches(12), Inches(0.7))
    title_frame = title_box.text_frame
    title_para = title_frame.paragraphs[0]
    title_para.text = title
    title_para.font.size = Pt(32)
    title_para.font.bold = True
    title_para.font.color.rgb = BLANCO
    title_para.alignment = PP_ALIGN.LEFT

    content_box = slide.shapes.add_textbox(Inches(0.7), Inches(1.6), Inches(12), Inches(5.2))
    text_frame = content_box.text_frame
    text_frame.word_wrap = True

    lines = content.split('\n')
    for i, line in enumerate(lines):
        if i == 0:
            p = text_frame.paragraphs[0]
        else:
            p = text_frame.add_paragraph()
        p.text = line
        p.font.size = Pt(20) if not line.startswith('**') else Pt(22)
        p.space_after = Pt(8)
        if line.startswith('**') and line.endswith('**'):
            p.text = line[2:-2]
            p.font.bold = True
            p.font.color.rgb = title_color
        elif line.startswith('- "') or (line.startswith('- ') and '"' in line):
            # Recorte / cita en estilo bloque de codigo
            p.text = '  ' + line[2:]
            p.font.size = Pt(16)
            p.font.name = 'Consolas'
            p.font.color.rgb = RGBColor(30, 30, 30)
        elif line.startswith('- '):
            p.text = '  ► ' + line[2:]
            p.font.color.rgb = NEGRO
        else:
            p.font.color.rgb = NEGRO

    return slide

def add_portada():
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_background(slide, AZUL_OSCURO)
    add_decorative_shape(slide, Inches(0), Inches(0), Inches(13.333), Inches(0.5), AZUL_CLARO)
    add_decorative_shape(slide, Inches(0), Inches(7), Inches(13.333), Inches(0.5), AZUL_CLARO)
    add_decorative_shape(slide, Inches(5.5), Inches(2.5), Inches(2.333), Inches(0.1), AMARILLO)

    title_box = slide.shapes.add_textbox(Inches(0.5), Inches(2.7), Inches(12.333), Inches(1.2))
    tf = title_box.text_frame
    p = tf.paragraphs[0]
    p.text = "AGENTS.md"
    p.font.size = Pt(54)
    p.font.bold = True
    p.font.color.rgb = BLANCO
    p.alignment = PP_ALIGN.CENTER

    p2 = tf.add_paragraph()
    p2.text = "Reglas del Asistente de IA"
    p2.font.size = Pt(40)
    p2.font.bold = True
    p2.font.color.rgb = AMARILLO
    p2.alignment = PP_ALIGN.CENTER

    sub_box = slide.shapes.add_textbox(Inches(0.5), Inches(4.2), Inches(12.333), Inches(1))
    tf2 = sub_box.text_frame
    p3 = tf2.paragraphs[0]
    p3.text = "Centro de Salud Administrativo - Convenciones para el equipo"
    p3.font.size = Pt(24)
    p3.font.color.rgb = RGBColor(200, 220, 255)
    p3.alignment = PP_ALIGN.CENTER

    team_box = slide.shapes.add_textbox(Inches(0.5), Inches(5.5), Inches(12.333), Inches(1))
    tf3 = team_box.text_frame
    p4 = tf3.paragraphs[0]
    p4.text = "Equipo: Bryan, Oscar, Percy, J.J."
    p4.font.size = Pt(22)
    p4.font.color.rgb = BLANCO
    p4.alignment = PP_ALIGN.CENTER
    p5 = tf3.add_paragraph()
    p5.text = "Expo Parcial - Octubre 2026"
    p5.font.size = Pt(18)
    p5.font.color.rgb = RGBColor(180, 200, 230)
    p5.alignment = PP_ALIGN.CENTER

# Slide 1 - Portada
add_portada()

# Slide 2 - Que es el AGENTS.md
add_slide('AGENTS.md - Que es', '''**AGENTS.md** es el archivo de instrucciones para el asistente de IA que trabaja en este repositorio.

Su objetivo:
- Construir un sistema mantenible, coherente, legible y técnicamente sólido
- Evitar la sobreingeniería y la complejidad innecesaria

**El código existente es una referencia de convenciones, no una garantía de que cada decisión anterior sea correcta.''',
    bg_color=RGBColor(255, 250, 245), title_color=NARANJA, accent_color=NARANJA)

# Slide 3 - Prioridad
add_slide('Prioridad de Decisiones', '''**Prioridad: corrección > claridad > simplicidad > mantenibilidad > optimización prematura.

La arquitectura debe facilitar la evolución del sistema, no convertirse en una obligación de agregar capas o abstracciones sin necesidad.''',
    bg_color=RGBColor(255, 250, 250), title_color=ROJO, accent_color=ROJO)

# Slide 4 - Control de alcance
add_slide('Control de Alcance', '''**Regla fundamental:**

- "Implementar únicamente lo solicitado explícitamente en la tarea actual."
- "No asumir que una funcionalidad pendiente, una fecha de entrega o una descripción arquitectónica autorizan su implementación."

No se realizan:
- Refactorizaciones extensas
- Cambios arquitectónicos globales
- Funcionalidades no solicitadas.''',
    bg_color=FONDO_ROJO, title_color=ROJO, accent_color=ROJO)

# Slide 5 - Reglas de Arquitectura
add_slide('Reglas de Arquitectura para el Asistente', '''**Dominio:**
- "Contiene entidades, objetos de valor, invariantes y reglas de negocio."
- "No crear servicios de dominio si la lógica pertenece naturalmente a una entidad."

**Adaptadores:**
- "Los controladores no deben contener reglas de negocio."
- "Los repositorios no deben decidir políticas de negocio."
- "No exponer entidades JPA directamente como respuestas HTTP."''',
    bg_color=FONDO_MORADO, title_color=MORADO, accent_color=MORADO)

# Slide 6 - Calidad, pruebas y flujo
add_slide('Calidad, Pruebas y Flujo', '''**Pruebas:**
- "Las pruebas deben verificar comportamientos y reglas relevantes, no simplemente aumentar su cantidad."

**Consistencia:**
- "Después de modificar código Java, ejecutar desde backend: `./mvnw spotless:check`"

**Flujo obligatorio:**
- "Antes de finalizar: revisar si existe una solución más simple, si las responsabilidades están correctamente distribuidas, si las pruebas relevantes pasan."''',
    bg_color=FONDO_AZUL, title_color=AZUL_OSCURO, accent_color=AZUL_OSCURO)

# Slide 7 - Principio fundamental
add_slide('Principio Fundamental', '''**"El mejor código no es el que utiliza más patrones, capas o abstracciones, sino el que resuelve correctamente el problema y puede ser comprendido, probado y modificado con el menor esfuerzo razonable."**

**Si dos soluciones son igualmente correctas y respetan los requisitos, preferir la que tenga menor complejidad y mayor claridad.**''',
    bg_color=FONDO_AREAL, title_color=AZUL_MEDIO, accent_color=AZUL_MEDIO)

prs.save('C:\\Users\\Juanjo\\centro-salud-jj\\Presentacion_Expo_Agents_Centro_Salud.pptx')
print("OK - Presentacion AGENTS creada")
