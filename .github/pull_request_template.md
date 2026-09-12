## Qué cambia
<!-- Lista corta de cambios concretos. Archivos o módulos, no líneas. -->
-

## Por qué
<!-- El problema o la necesidad que motiva el cambio. Si viene del
     intento anterior, cita el commit de la rama historial. -->

## Cómo se verificó
<!-- Comandos ejecutados y resultado. Capturas si hay UI o pipeline. -->
- [ ] `mvn clean test` en local: BUILD SUCCESS
- [ ] Check `Build` en verde en este PR
- [ ] Quality gate de Sonar en verde (desde PR 5)

## Riesgos y pendientes
<!-- Qué NO cubre este PR, qué podría romperse, qué se deja para
     después. Si no hay, escribir "Ninguno identificado" y por qué. -->

## Checklist del working agreement
- [ ] Rama creada desde `main` actualizado, nombre `feature/<descripcion>`
- [ ] Commits con prefijo Conventional Commits, uno por cambio
- [ ] Rama al día con `main`, sin conflictos
- [ ] Al mergear: Squash and merge y borrar la rama
