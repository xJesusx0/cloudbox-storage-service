# Cloudbox Storage Service

Este proyecto es una API construida con Spring Boot 4 para almacenar archivos en distintos sistemas (S3, FTP, SMB, NFS). A continuación se explica cómo construir y ejecutar la aplicación mediante Docker.

## 1. Construir la Imagen Docker

El comando para construir la imagen es:

```bash
docker build -t xjesusx0/cloudbox-storage-service:0.0.1-SNAPSHOT .
```

Una vez construida, si deseas subirla a Docker Hub, ejecutas el push como un comando separado:

```bash
docker push xjesusx0/cloudbox-storage-service:0.0.1-SNAPSHOT
```

## 2. Variables de Entorno

**¿Tenías que pasarle las variables de entorno al construir (`docker build`)?**
**No**, las variables de entorno como los accesos a la base de datos o a MinIO (S3) no se necesitan al momento de *construir* la imagen (al menos no en la etapa de empaquetado normal de Java), sino al momento de **ejecutar / correr** el contenedor.

El proyecto requiere las siguientes variables de entorno para conectarse correctamente a MinIO:
- `MINIO_ENDPOINT`
- `MINIO_USERNAME`
- `MINIO_PASSWORD`
- `MINIO_REGION`
- `MINIO_BUCKET_NAME`

## 3. Ejecutar el Contenedor

Para que la aplicación tome estas variables al ejecutarse, puedes usar el flag `--env-file` de Docker o pasarlas una por una con `-e`.

### Opción A: Usar tu archivo `.env` existente (Recomendada)

Si estás situado en la raíz del proyecto y tu archivo `.env` está en `src/main/resources/.env`, puedes pasarle ese archivo directamente al arrancar el contenedor:

```bash
docker run -d \
  --name cloudbox-storage-service \
  -p 8080:8080 \
  --env-file src/main/resources/.env \
  xjesusx0/cloudbox-storage-service:0.0.1-SNAPSHOT
```

### Opción B: Pasar las variables explícitamente

```bash
docker run -d \
  --name cloudbox-storage-service \
  -p 8080:8080 \
  -e MINIO_ENDPOINT="http://192.168.56.101:9000" \
  -e MINIO_USERNAME="miniouser" \
  -e MINIO_PASSWORD="12345678" \
  -e MINIO_REGION="us-east-1" \
  -e MINIO_BUCKET_NAME="bucketprueba" \
  xjesusx0/cloudbox-storage-service:0.0.1-SNAPSHOT
```

### Explicación de los parámetros:
- `-d`: Ejecuta el contenedor en modo _detached_ (en segundo plano).
- `--name cloudbox-storage-service`: Le asigna un nombre amigable al contenedor (opcional, pero buena práctica).
- `-p 8080:8080`: Mapea el puerto `8080` de tu máquina host al puerto `8080` dentro del contenedor de Spring Boot.
- `--env-file`: Lee un archivo con las variables de entorno necesarias para la ejecución.

## 4. Ver los Logs de la Aplicación

Si necesitas ver si la aplicación arrancó correctamente o por qué falló:

```bash
docker logs -f cloudbox-storage-service
```
