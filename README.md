# 📚 Grupo 4 – Préstamos (Integrado)

Bienvenido al microservicio **Grupo 4 conectado**, encargado de gestionar los préstamos y las reservas dentro de la Biblioteca Digital. Esta versión apunta a los servicios reales de la universidad 🚀 (MySQL de G1, JWT, Catálogo de G3).

---
## ✨ Características clave
- 📖 Administración completa del ciclo de vida del préstamo (solicitar, aprobar, renovar, devolver, listar).
- ⏳ Manejo automático de préstamos vencidos y reservas expiradas.
- ⚙️ Reglas de negocio leídas dinámicamente desde la tabla `configuracion` (sin valores “hardcode”). 
- 🛡️ Integración con JWT (G2) y Catálogo real (G3).

---
## 🏗️ Arquitectura a simple vista
```
Cliente -> API G4 -> MySQL (G1)
               \-> JWT (G2)
               \-> Catálogo G3 (reservar/devolver unidades)
```

---
## 🛠️ Requisitos
- Java 17 ☕️
- Maven 3.9+
- Acceso a las BDs/servicios reales:
  - `spring.datasource.url`: MySQL G1  
  - `jwt.secret`: mismo secreto que G2 y G3  
  - `catalog.base-url`: endpoint del Catálogo (Render, K8s, etc.)  

---
## 🚀 Cómo ejecutar
```bash
# 1. Instala dependencias y corre la suite completa
./mvnw -q test

# 2. Arranca el servicio (perfil por defecto = producción)
./mvnw spring-boot:run
```
El servicio escucha en el puerto `8081` (puedes cambiarlo en `src/main/resources/application.properties`).  

---
## 🧪 Pruebas automáticas
| Suite | Descripción |
|-------|-------------|
| `LoanService*Test` | Casos de solicitar, devolver, aprobar y renovar préstamos. |
| `ReservationService*Test` | Creación, cancelación, expiración y cola de reservas. |
| `ApplicationTests` | Carga de contexto Spring Boot. |

Ejecuta todo con:
```bash
./mvnw -q test
```

---
## 📝 Endpoints principales
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/loans` | Solicitar un préstamo. |
| `PUT`  | `/api/loans/{id}/renew` | Renovar préstamo (máx una vez). |
| `PUT`  | `/api/loans/{id}/return` | Devolver el ejemplar. |
| `GET`  | `/api/loans/my-loans` | Ver préstamos del usuario autenticado. |
| `POST` | `/api/reservations` | Crear una reserva (si no hay disponibilidad). |
| `DELETE` | `/api/reservations/{id}` | Cancelar una reserva. |

> Usa Swagger en `http://localhost:8081/swagger-ui.html` 😎  

---
## 🔄 Synced con la versión “aislada”
Toda la lógica de negocio es compartida con `Grupo 4- Aislado`; lo único que cambia es la capa de integración (el aislado usa H2 + mocks). Si necesitas hacer pruebas locales sin depender de G1/G3, usa esa versión 👉 [ver README](../Grupo 4- Aislado/Grupo 4- Aislado/Grupo 4- Aislado/README.md).

---
## 🆘 Soporte / Contacto
- Grupo 4 – Sistemas de Préstamo 📬 `grupo4@universidad.edu`
- Reporta issues en el tablero interno o vía Teams.

¡Feliz coding y que los libros te acompañen! 📖✨
