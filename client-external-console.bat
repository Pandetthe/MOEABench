call mvnw install -DskipTests
call mvnw -pl client spring-boot:run "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"