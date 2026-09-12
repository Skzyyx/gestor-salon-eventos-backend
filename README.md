# 🎪 Gestor de Salones de Eventos — Backend API

Backend RESTful desarrollado con **Java 17** y **Spring Boot 3.4.1** para la administración integral y multi-inquilino (*multi-tenant*) de salones de eventos, fiestas y recepciones.

Permite gestionar catálogos de servicios, paquetes, cotizaciones, turnos/agenda, clientes, personal de trabajo con control de acceso basado en roles y permisos atómicos (RBAC), y subida de imágenes a la nube.

---

## 🚀 Tecnologías Principales

- **Lenguaje:** Java 17 (LTS)
- **Framework:** Spring Boot 3.4.1
  - **Spring Web**: Construcción de API RESTful
  - **Spring Data JPA / Hibernate**: Persistencia y mapeo relacional
  - **Spring Security 6 & JJWT (0.12.6)**: Autenticación sin estado (*stateless*) mediante tokens JWT y encriptación BCrypt
  - **Spring Validation**: Validación declarativa de DTOs (`@Valid`, Bean Validation)
  - **Spring Mail**: Notificaciones y soporte de correo electrónico
- **Base de Datos:** MySQL 8.x
- **Migraciones:** Flyway (`flyway-mysql`) para versionado y evolución controlada del esquema
- **Almacenamiento de Multimedia:** Cloudinary (HTTP44) con validación MIME segura mediante **Apache Tika (2.9.2)**
- **Documentación:** Springdoc OpenAPI (Swagger UI) 2.7.0
- **Herramientas de Productividad:** Lombok & Maven Compiler Plugin con soporte explícito de annotation processing

---

## 🏛️ Arquitectura y Modelo de Seguridad

### 1. Multi-inquilino (*Multi-tenancy*) y Aislamiento de Datos

- **Plataforma global vs. Negocios individuales:** La plataforma puede albergar múltiples salones de eventos.
- Cada recurso (servicios, categorías, turnos, clientes, equipo) está asociado a un `negocio_id` específico, garantizando que un salón nunca acceda a los datos de otro.

### 2. Jerarquía de Roles y Control de Acceso (RBAC)

El sistema implementa tres niveles de usuario:

| Rol                      | Ámbito                                   | Descripción                                                                                                                                             |
| :----------------------- | :---------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`SUPERADMIN`** | Plataforma global (`negocio_id = NULL`) | Máxima autoridad del sistema. Encargado de dar de alta negocios y provisionar a sus administradores iniciales. Acceso exclusivo a `/api/v1/admin/**`. |
| **`ADMIN`**      | Negocio específico                       | Dueño o gerente general de un salón de eventos. Posee permisos absolutos automáticos sobre todos los recursos de su negocio.                          |
| **`EMPLEADO`**   | Negocio específico                       | Colaborador operativo (recepcionista, coordinador, etc.). Sus permisos se configuran a la medida mediante**`RolNegocio`**.                       |

### 3. Catálogo de Permisos Atómicos (`Permiso`)

Los empleados no reciben permisos arbitrarios; se configuran mediante roles de negocio compuestos por autoridades de tipo `PERM_*`:

- **Servicios:** `PERM_SERVICIO_VER`, `PERM_SERVICIO_CREAR`, `PERM_SERVICIO_EDITAR`, `PERM_SERVICIO_ELIMINAR`
- **Categorías:** `PERM_CATEGORIA_VER`, `PERM_CATEGORIA_GESTIONAR`
- **Paquetes:** `PERM_PAQUETE_VER`, `PERM_PAQUETE_CREAR`, `PERM_PAQUETE_EDITAR`, `PERM_PAQUETE_ELIMINAR`
- **Clientes:** `PERM_CLIENTE_VER`, `PERM_CLIENTE_CREAR`, `PERM_CLIENTE_EDITAR`, `PERM_CLIENTE_ELIMINAR`
- **Turnos / Agenda:** `PERM_TURNO_VER`, `PERM_TURNO_CREAR`, `PERM_TURNO_EDITAR`, `PERM_TURNO_CANCELAR`
- **Perfil de Salón:** `PERM_NEGOCIO_VER_PERFIL`, `PERM_NEGOCIO_EDITAR_PERFIL`
- **Equipo y Roles:** `PERM_EQUIPO_VER`, `PERM_EQUIPO_GESTIONAR`, `PERM_ROL_VER`, `PERM_ROL_GESTIONAR`

---

## 📁 Estructura del Proyecto

```text
gestor-salon-eventos-backend/
├── pom.xml                               # Dependencias Maven y configuración de plugins
├── .gitignore                            # Exclusiones de Git (Maven, IDEs, secretos)
└── src/
    ├── main/
    │   ├── java/mx/gestorsalon/
    │   │   ├── GestorSalonApplication.java # Clase principal Spring Boot
    │   │   ├── config/                   # Configuración de Seguridad, CORS y Cloudinary
    │   │   ├── controller/               # Controladores REST expuestos
    │   │   ├── dto/                      # Data Transfer Objects (peticiones y respuestas)
    │   │   ├── exception/                # Manejador global de excepciones (@ControllerAdvice)
    │   │   ├── mapper/                   # Mapeadores Entity <-> DTO
    │   │   ├── model/                    # Entidades JPA y Enums (Rol, Permiso, TipoCobro)
    │   │   ├── repository/               # Repositorios Spring Data JPA
    │   │   ├── security/                 # Filtro JWT, Token Provider y UserDetailsService
    │   │   ├── service/                  # Lógica de negocio de cada módulo
    │   │   └── util/                     # Utilidades de seguridad y contexto
    │   └── resources/
    │       ├── application.yml           # Configuración base del sistema
    │       ├── application-dev.yml.example   # Plantilla para conexión a BD de desarrollo (Aiven / Local)
    │       ├── application-local.yml.example # Plantilla para secretos locales (JWT, Cloudinary)
    │       ├── application-prod.yml      # Configuración de producción por variables de entorno
    │       ├── application-test.yml      # Configuración para pruebas
    │       └── db/migration/             # Migraciones Flyway (V1__... a V12__...)
    └── test/                             # Pruebas unitarias y de integración
```

text---

## ⚙️ Requisitos Previos

- **Java Development Kit (JDK):** Versión 17 o superior instalada.
- **Maven:** Versión 3.8 o superior (o el Maven embebido en tu IDE favorito).
- **Base de Datos:** Instancia MySQL 8 accesible o servicio en la nube (Aiven, PlanetScale, etc.).
- **Cuenta Cloudinary:** Para almacenamiento de imágenes y logotipos de los salones.

---

## 🛠️ Configuración y Puesta en Marcha

### 1. Clonar el repositorio

```bash
git clone https://github.com/Skzyyx/gestor-salon-eventos-backend.git
cd gestor-salon-eventos-backend
```

bash### 2. Configurar los Archivos de Entorno y Secretos Locales

Por razones de seguridad, las contraseñas de bases de datos (Aiven), secretos JWT y credenciales de Cloudinary **están en `.gitignore` y NO se versionan en Git**. Se proporcionan archivos de plantilla `.example` que sí se versionan:

#### A) Configurar Base de Datos de Desarrollo (`application-dev.yml`)

1. Copia la plantilla de desarrollo:
   ```bash
   # En Windows PowerShell:
   Copy-Item src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml

   # En Linux / macOS:
   cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
   ```

   bash
2. Rellena la URL, usuario y contraseña de tu base de datos de Aiven o MySQL local.

#### B) Configurar Claves de la Aplicación (`application-local.yml`)

1. Copia la plantilla de secretos locales:

   ```bash
   # En Windows PowerShell:
   Copy-Item src/main/resources/application-local.yml.example src/main/resources/application-local.yml

   # En Linux / macOS:
   cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
   ```

   bash
2. Abre `src/main/resources/application-local.yml` y rellena tus claves de JWT y Cloudinary:

   ```yaml
   app:
     jwt:
       secret: TU_CLAVE_SECRETA_BASE64_DE_AL_MENOS_256_BITS
     cloudinary:
       cloud-name: tu-cloud-name
       api-key: "tu-api-key"
       api-secret: tu-api-secret
   ```

   yaml

> 💡 **Tip para generar tu clave JWT:**
> Puedes generar una clave aleatoria en Base64 de 384 bits con:
>
> ```bash
> openssl rand -base64 48
> ```
>
> bash

### 3. Perfiles de Spring Activos

Por defecto en `application.yml` se encuentran activos:

```yaml
spring:
  profiles:
    active: dev,local
```

yaml- `dev`: Se conecta a la base de datos de desarrollo y habilita logs detallados de Hibernate.
- `local`: Carga el archivo `application-local.yml` con tus secretos locales.
- `prod`: Para despliegues (Docker, servidores en la nube) donde todo se inyecta mediante variables de entorno del sistema (`DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, etc.).

### 4. Compilación y Ejecución

#### Desde la Terminal (Maven):

```bash
mvn clean spring-boot:run
```

bash#### Empaquetar en archivo ejecutable JAR:

```bash
mvn clean package -DskipTests
java -jar target/gestor-salon-eventos-0.1.0-SNAPSHOT.jar
```

bash#### Desde tu IDE (IntelliJ IDEA, VS Code, Eclipse):

1. Importa el proyecto como proyecto Maven existente.
2. Asegúrate de habilitar **Annotation Processing** en las opciones del IDE para que Lombok genere los métodos y constructores en tiempo de compilación.
3. Ejecuta la clase `GestorSalonApplication.java`.

---

## 🔑 Usuario Inicial (Bootstrap de la Plataforma)

Al arrancar la aplicación por primera vez, Flyway ejecuta automáticamente la migración `V6__seed_superadmin.sql` creando el primer usuario administrador de la plataforma:

- **Correo:** `super@gestorsalon.com`
- **Contraseña:** `SuperAdmin123!`
- **Rol:** `SUPERADMIN`

### Flujo inicial recomendado:

1. Inicia sesión con `POST /api/v1/auth/login` usando las credenciales del Superadmin.
2. Copia el token JWT de la respuesta y úsalo en el encabezado `Authorization: Bearer <TOKEN>`.
3. Crea un nuevo salón de eventos y su dueño inicial con `POST /api/v1/admin/negocios`.
4. Inicia sesión con el correo y contraseña del administrador creado para comenzar a gestionar el salón (servicios, paquetes, empleados, turnos).

---

## 📚 Endpoints Principales de la API

La URL base de todos los endpoints es: `http://localhost:8080/api/v1`

### 🔐 Autenticación (`/auth`)

| Método  | Endpoint        | Permiso requerido | Descripción                                                 |
| :------- | :-------------- | :---------------- | :----------------------------------------------------------- |
| `POST` | `/auth/login` | *Público*      | Autentica usuario y devuelve token JWT con datos de sesión. |
| `GET`  | `/auth/me`    | Autenticado       | Devuelve los datos y permisos del usuario del token actual.  |

### 🏢 Superadministrador (`/admin`)

| Método  | Endpoint            | Permiso requerido   | Descripción                                           |
| :------- | :------------------ | :------------------ | :----------------------------------------------------- |
| `POST` | `/admin/negocios` | `ROLE_SUPERADMIN` | Da de alta un nuevo negocio y su cuenta admin inicial. |
| `GET`  | `/admin/negocios` | `ROLE_SUPERADMIN` | Lista todos los salones de eventos registrados.        |

### 🏬 Mi Negocio (`/negocios`)

| Método | Endpoint         | Permiso requerido | Descripción                                                        |
| :------ | :--------------- | :---------------- | :------------------------------------------------------------------ |
| `GET` | `/negocios/me` | Autenticado       | Obtiene la información del negocio del usuario autenticado.        |
| `PUT` | `/negocios/me` | Autenticado       | Actualiza datos del negocio (nombre, dirección, teléfonos, logo). |

### 🛎️ Servicios del Salón (`/servicios`)

| Método    | Endpoint            | Permiso requerido          | Descripción                                                       |
| :--------- | :------------------ | :------------------------- | :----------------------------------------------------------------- |
| `GET`    | `/servicios`      | `PERM_SERVICIO_VER`      | Lista los servicios activos del salón.                            |
| `POST`   | `/servicios`      | `PERM_SERVICIO_CREAR`    | Crea un nuevo servicio (nombre, costo, tipo de cobro, categoría). |
| `PUT`    | `/servicios/{id}` | `PERM_SERVICIO_EDITAR`   | Modifica los datos de un servicio existente.                       |
| `DELETE` | `/servicios/{id}` | `PERM_SERVICIO_ELIMINAR` | Desactiva (baja lógica) un servicio.                              |

### 🏷️ Categorías de Servicios (`/categorias`)

| Método    | Endpoint             | Permiso requerido            | Descripción                                        |
| :--------- | :------------------- | :--------------------------- | :-------------------------------------------------- |
| `GET`    | `/categorias`      | `PERM_CATEGORIA_VER`       | Obtiene el catálogo de categorías del salón.     |
| `POST`   | `/categorias`      | `PERM_CATEGORIA_GESTIONAR` | Registra una nueva categoría.                      |
| `PUT`    | `/categorias/{id}` | `PERM_CATEGORIA_GESTIONAR` | Actualiza el nombre/descripción de una categoría. |
| `DELETE` | `/categorias/{id}` | `PERM_CATEGORIA_GESTIONAR` | Desactiva una categoría.                           |

### 📦 Paquetes de Eventos (`/paquetes`)

| Método    | Endpoint           | Permiso requerido                      | Descripción                                                         |
| :--------- | :----------------- | :------------------------------------- | :------------------------------------------------------------------- |
| `GET`    | `/paquetes`      | Autenticado /`PERM_PAQUETE_VER`      | Lista los paquetes promocionales y combos configurados.              |
| `POST`   | `/paquetes`      | Autenticado /`PERM_PAQUETE_CREAR`    | Crea un paquete con lista detallada de servicios incluidos y precio. |
| `PUT`    | `/paquetes/{id}` | Autenticado /`PERM_PAQUETE_EDITAR`   | Modifica la configuración de un paquete.                            |
| `DELETE` | `/paquetes/{id}` | Autenticado /`PERM_PAQUETE_ELIMINAR` | Desactiva un paquete.                                                |

### 👥 Clientes (`/clientes`)

| Método    | Endpoint           | Permiso requerido                      | Descripción                                                  |
| :--------- | :----------------- | :------------------------------------- | :------------------------------------------------------------ |
| `GET`    | `/clientes`      | Autenticado /`PERM_CLIENTE_VER`      | Consulta el directorio de clientes del salón.                |
| `POST`   | `/clientes`      | Autenticado /`PERM_CLIENTE_CREAR`    | Registra un nuevo cliente (nombre, teléfono, correo, notas). |
| `PUT`    | `/clientes/{id}` | Autenticado /`PERM_CLIENTE_EDITAR`   | Actualiza la ficha de un cliente.                             |
| `DELETE` | `/clientes/{id}` | Autenticado /`PERM_CLIENTE_ELIMINAR` | Desactiva un cliente del sistema.                             |

### ⏰ Turnos y Horarios (`/turnos`)

| Método    | Endpoint         | Permiso requerido | Descripción                                                                |
| :--------- | :--------------- | :---------------- | :-------------------------------------------------------------------------- |
| `GET`    | `/turnos`      | Autenticado       | Obtiene la configuración de turnos (mañana, tarde, noche, día completo). |
| `POST`   | `/turnos`      | Autenticado       | Configura un nuevo turno y horario operativo.                               |
| `PUT`    | `/turnos/{id}` | Autenticado       | Edita las horas de inicio/fin o nombre del turno.                           |
| `DELETE` | `/turnos/{id}` | Autenticado       | Elimina un turno.                                                           |

### 👔 Equipo de Trabajo y Roles (`/equipo` & `/roles-negocio`)

| Método   | Endpoint                | Permiso requerido                            | Descripción                                                  |
| :-------- | :---------------------- | :------------------------------------------- | :------------------------------------------------------------ |
| `GET`   | `/equipo`             | `PERM_EQUIPO_VER`                          | Lista los colaboradores registrados para el negocio.          |
| `POST`  | `/equipo`             | `PERM_EQUIPO_GESTIONAR`                    | Da de alta un nuevo empleado y le asigna su rol.              |
| `PUT`   | `/equipo/{id}`        | `PERM_EQUIPO_GESTIONAR`                    | Actualiza datos y rol asignado a un trabajador.               |
| `PATCH` | `/equipo/{id}/estado` | `PERM_EQUIPO_GESTIONAR`                    | Activa o desactiva la cuenta de un trabajador.                |
| `GET`   | `/roles-negocio`      | `PERM_ROL_VER` o `PERM_EQUIPO_GESTIONAR` | Lista los roles predeterminados y personalizados del negocio. |

### 🖼️ Subida de Imágenes (`/images`)

| Método  | Endpoint           | Permiso requerido | Descripción                                                                                    |
| :------- | :----------------- | :---------------- | :---------------------------------------------------------------------------------------------- |
| `POST` | `/images/upload` | Autenticado       | Recibe un archivo `multipart/form-data` ("file"), valida formato y lo almacena en Cloudinary. |

---

## 📖 Documentación Interactiva (Swagger / OpenAPI)

Con la aplicación en ejecución, puedes consultar y probar interactivamente los endpoints en tu navegador:

- **Swagger UI:** [http://localhost:8080/api/v1/swagger-ui/index.html](http://localhost:8080/api/v1/swagger-ui/index.html)
- **OpenAPI JSON:** [http://localhost:8080/api/v1/v3/api-docs](http://localhost:8080/api/v1/v3/api-docs)

---

## 🌐 Configuración CORS

Por defecto, la API tiene configurado el intercambio de recursos de origen cruzado (CORS) para comunicarse con clientes frontend modernos desarrollados en Vite / React:

- Origen permitido: `http://localhost:5173`
- Métodos: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
- Credenciales: Habilitadas (`AllowCredentials: true`)

Para agregar otros orígenes (por ejemplo, el dominio de producción del frontend), modifica la clase `SecurityConfig.java`.

---

## 🧪 Pruebas Automatizadas

Ejecutar la suite de tests unitarios y de integración con Maven:

```bash
mvn clean test
```

bash---

## 📋 Documentación del Curso (DevOps)

Este repositorio es el proyecto del equipo para la materia de DevOps. La documentación del proceso vive junto al código:

- [`working-agreement.md`](working-agreement.md): acuerdos de trabajo del equipo y estrategia de branching (**GitHub Flow**).
- [`docs/backlog.md`](docs/backlog.md): backlog de capacidades DevOps por semana.
- [`docs/adr/`](docs/adr/): Architecture Decision Records (plantilla en `0000-template.md`).
- [`docs/evidence/sprint-XX.md`](docs/evidence/): evidencia acumulativa por Sprint. No se sobrescriben evidencias anteriores; cada Sprint conserva su propio archivo.
- Rama [`historial`](https://github.com/Skzyyx/gestor-salon-eventos-backend/tree/historial): archivo de solo lectura con el trabajo exploratorio de CI, SonarQube y tests realizado el 11 de septiembre de 2026 antes de adoptar el flujo de PRs. Se conserva para que los runs de GitHub Actions y los análisis de SonarQube de esa fecha sigan apuntando a commits existentes. No se mergea, no se rebasea y no se borra; el trabajo válido se rehízo en `main` mediante PRs durante el Sprint 1.

Antes de que aplique en el Sprint correspondiente, en las secciones de PR, pipeline, deployment o infraestructura se registra: `N/A — todavía no corresponde a este Sprint.`
