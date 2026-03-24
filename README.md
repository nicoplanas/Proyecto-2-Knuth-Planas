# Proyecto 2 - Simulador Virtual de Sistema de Archivos Concurrente

Base funcional en Java 21 y Swing para NetBeans/Maven que cubre los componentes pedidos en el enunciado:

- Sistema de archivos jerarquico con directorios y archivos.
- Simulacion de disco por bloques con asignacion encadenada.
- Tabla de asignacion de archivos.
- Cola de procesos de E/S con estados y planificacion FIFO, SSTF, SCAN y C-SCAN.
- Locks compartidos y exclusivos.
- Journaling para create y delete con simulacion de fallo y recuperacion.
- Persistencia del estado a JSON.
- Carga de escenarios de prueba desde JSON.
- GUI Swing con JTree, JTable, panel de disco y log de eventos.

## Ejecutar

Abrir el proyecto Maven en NetBeans o ejecutar:

```bash
mvn compile
mvn exec:java
```

## Estructura

- `src/main/java/com/unimet/so/proyecto2/app`: punto de entrada.
- `src/main/java/com/unimet/so/proyecto2/ds`: estructuras de datos propias.
- `src/main/java/com/unimet/so/proyecto2/model`: entidades del simulador.
- `src/main/java/com/unimet/so/proyecto2/engine`: logica de negocio.
- `src/main/java/com/unimet/so/proyecto2/persistence`: serializacion y carga de escenarios.
- `src/main/java/com/unimet/so/proyecto2/ui`: interfaz grafica.
- `src/main/resources/samples`: JSON de prueba.

## Notas

- La base evita `ArrayList`, `Queue`, `Stack` y `Vector` para la gestion del simulador.
- El proyecto queda listo para abrirse y extenderse en NetBeans.
- GitHub remoto, ramas y PRs no pueden crearse desde aqui; solo se deja el proyecto inicializado para que lo subas.