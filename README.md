# UNO Game Server

Servidor independiente para el juego de cartas UNO, diseñado para ser desplegado en Railway.

## Estructura del Proyecto

- `uno-shared/`: Librería compartida con modelos y clases de red
- `uno-server/`: Servidor TCP con validaciones completas

## Construcción Local

```bash
# Construir shared library
cd uno-shared
mvn clean install

# Construir servidor
cd ../uno-server
mvn clean package

# Ejecutar servidor
java -jar target/uno-server-1.0-SNAPSHOT.jar
```

## Deployment en Railway

Este proyecto está configurado para desplegarse automáticamente en Railway usando el `Dockerfile` incluido.

## Variables de Entorno

- `PORT`: Puerto en el que escucha el servidor (default: 12345)

## Características

- ✅ Validación completa de inputs
- ✅ Manejo de errores robusto
- ✅ Soporte multi-cliente
- ✅ Protocolo JSON sobre TCP
