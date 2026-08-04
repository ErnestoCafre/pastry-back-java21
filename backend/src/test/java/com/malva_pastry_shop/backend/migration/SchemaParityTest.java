package com.malva_pastry_shop.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Que el esquema que arman las migraciones sea el que las entidades esperan.
 *
 * <p>Prod y dev no comparten camino de esquema: prod corre Flyway con
 * {@code ddl-auto=none}, dev deja que Hibernate lo genere con
 * {@code ddl-auto=create} y lo siembra con {@code DataSeeder}. Los dos caminos
 * arrancan bien por separado, y por eso pueden separarse sin que nada falle.
 * Alguien agrega un campo a una entidad, dev lo levanta solo en el siguiente
 * arranque, la suite entera sigue en verde, y la columna no existe en ninguna
 * migración. Eso no se descubre hasta el deploy.
 *
 * <p>Hasta ahora nada en la suite corría las migraciones: la única mención de
 * Flyway en {@code src/test} era un {@code spring.flyway.enabled=false}. Este
 * test cierra ese hueco por el único lado que sirve: aplicando V1..V6 sobre un
 * Postgres virgen y comparando el resultado contra el metamodelo de Hibernate.
 *
 * <p><b>Por qué un contenedor y no H2.</b> Las migraciones son Postgres y nada
 * más: V5 es un bloque PL/pgSQL ({@code DO $$ ... $$}), V2 usa
 * {@code ON CONFLICT} y V6 aliasea la tabla dentro de un {@code DELETE}.
 * Verificarlas contra otro motor sería verificar otra cosa —exactamente el
 * problema que este test viene a resolver—.
 *
 * <p>La imagen es {@code postgres:13}, que es el piso que declara el README, no
 * la última. Si una migración empieza a usar algo de 14+, acá se ve.
 *
 * <p><b>Alcance.</b> Esto verifica la instalación limpia: base vacía, V1..V6 de
 * corrido. No cubre el camino de {@code baseline-on-migrate}, y es a propósito
 * —ese camino está roto y no se puede arreglar sin editar migraciones ya
 * aplicadas, así que un test lo dejaría fijado en vez de resuelto—. Está
 * documentado en {@code V1__create_schema.sql} y en
 * {@code application-prod.properties}.
 *
 * <p>Sin Docker el test se saltea en vez de fallar. Un skip queda visible en la
 * salida de surefire; un verde falso, no.
 */
@SpringBootTest
@Testcontainers
// Ni dev ni prod: dev activaría DataSeeder, que sembraría por encima de lo que
// dejó R__ y volvería inútil la comparación.
@ActiveProfiles("migration")
@TestPropertySource(properties = {
        // application.properties trae flyway apagado y ddl-auto=create, que es
        // justo el camino que este test NO quiere ejercitar.
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        // none, no validate: si el esquema y las entidades divergen, se quiere
        // el diff legible que arma este test, no un fallo al levantar el
        // contexto que deja a los cinco casos sin correr.
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.format_sql=false"
})
@EnabledIf("dockerAvailable")
@DisplayName("Paridad entre el esquema de Flyway y las entidades")
class SchemaParityTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:13-alpine");

    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    // ---------- 1. El invariante que cierra el hueco ----------

    /**
     * Toda columna que una entidad mapea existe en el esquema que dejó Flyway.
     *
     * <p>Es la dirección que importa. Que sobre una columna en la base no rompe
     * nada —una que quedó de un campo retirado sigue ahí, nullable, sin que
     * nadie la lea—. Que falte una es un {@code INSERT} que explota en prod y
     * en ningún otro lado.
     *
     * <p>Los nombres salen del metamodelo de Hibernate, no de leer las
     * anotaciones: es la misma fuente que usa el runtime para armar el SQL, así
     * que si Hibernate va a pedir la columna, acá aparece.
     */
    @Test
    @DisplayName("toda columna que mapea una entidad existe en el esquema migrado")
    void everyMappedColumnSurvivedTheMigrations() {
        Map<String, Set<String>> expected = mappedColumns();
        Map<String, Set<String>> actual = migratedColumns();

        Map<String, Set<String>> missing = new TreeMap<>();
        expected.forEach((table, columns) -> {
            Set<String> present = actual.getOrDefault(table, Set.of());
            Set<String> absent = new TreeSet<>(columns);
            absent.removeAll(present);
            if (!absent.isEmpty()) {
                missing.put(table, absent);
            }
        });

        assertThat(missing)
                .as("""
                        Hay columnas que las entidades mapean y que ninguna migración crea.

                        Dev no lo nota: ddl-auto=create se las inventa en cada arranque. Prod
                        corre Flyway con ddl-auto=none y no tiene de dónde sacarlas, así que
                        la consulta falla recién en el deploy.

                        Se arregla con una migración nueva (V7__...sql), no tocando V1: las
                        versionadas ya aplicadas no se editan.
                        """)
                .isEmpty();
    }

    /**
     * Y que la tabla entera no falte, que es el mismo error un nivel más
     * arriba y da un mensaje mucho peor si se descubre por la columna.
     */
    @Test
    @DisplayName("toda tabla que mapea una entidad existe en el esquema migrado")
    void everyMappedTableSurvivedTheMigrations() {
        Set<String> expected = new TreeSet<>(mappedColumns().keySet());
        Set<String> actual = migratedColumns().keySet();
        expected.removeAll(actual);

        assertThat(expected)
                .as("Hay entidades cuya tabla no la crea ninguna migración.")
                .isEmpty();
    }

    /**
     * Que los dos casos de arriba tengan algo que comparar.
     *
     * <p>Es el control que hace falta, y no es el de la lógica del diff —restar
     * dos conjuntos no se rompe—. Es que {@code mappedColumns()} camina el
     * metamodelo de Hibernate por una API que puede cambiar de forma entre
     * versiones. Si un día devolviera un mapa vacío, los dos casos anteriores
     * pasarían sin comparar nada y en verde, que es la peor manera de fallar.
     *
     * <p>La lista está escrita a mano a propósito. Una entidad nueva la hace
     * fallar, y esa es la conversación que se quiere forzar: quien agrega una
     * entidad tiene que agregar también su migración.
     */
    @Test
    @DisplayName("el metamodelo entrega las doce tablas del dominio")
    void theComparisonHasSomethingToCompare() {
        assertThat(mappedColumns().keySet())
                .as("""
                        El metamodelo de Hibernate no entregó las tablas esperadas.

                        Si están de más: hay una entidad nueva. Necesita una migración
                        versionada, y esta lista se actualiza junto con ella.

                        Si están de menos: se rompió el recorrido del metamodelo, y los dos
                        casos de paridad de arriba están comparando contra el vacío.
                        """)
                .containsExactlyInAnyOrder(
                        "roles", "users",
                        "categories", "ingredients", "products", "product_ingredients",
                        "sales", "sale_ingredients",
                        "tags", "product_tags",
                        "storefront_sections", "storefront_section_products");

        assertThat(mappedColumns().get("users"))
                .as("Una tabla concreta, para que el recorrido no pueda entregar tablas "
                        + "vacías y seguir contando doce.")
                .contains("id", "email", "password_hash", "role_id");
    }

    // ---------- 2. Que las migraciones hayan corrido de verdad ----------

    /**
     * El control negativo de los dos casos de arriba.
     *
     * <p>Si Flyway quedara apagado por un cambio de configuración, la base
     * estaría vacía, {@code migratedColumns()} devolvería un mapa vacío y los
     * dos tests anteriores fallarían fuerte. Pero si alguien además "arreglara"
     * eso poniendo {@code ddl-auto=create} acá, pasarían por el motivo
     * equivocado: el esquema lo habría armado Hibernate, no las migraciones.
     * Este caso ancla que el esquema salió de V1..V6.
     */
    @Test
    @DisplayName("las seis migraciones versionadas se aplicaron sin fallar")
    void everyVersionedMigrationRan() {
        List<Map<String, Object>> applied = jdbc.queryForList(
                "SELECT version, description, success FROM flyway_schema_history "
                        + "WHERE version IS NOT NULL ORDER BY installed_rank");

        assertThat(applied)
                .as("El historial de Flyway no tiene las seis migraciones versionadas.")
                .hasSize(6);
        assertThat(applied).allSatisfy(row ->
                assertThat(row.get("success")).as("Migración fallida: %s", row).isEqualTo(true));
        assertThat(applied.stream().map(row -> row.get("version")).toList())
                .containsExactly("1", "2", "3", "4", "5", "6");
    }

    // ---------- 3. Que cada migración haya hecho lo que dice ----------

    /**
     * Lo que la comparación de columnas no puede ver.
     *
     * <p>Una entidad borrada deja de pedir su tabla, así que
     * {@code everyMappedColumnSurvivedTheMigrations} se queda callado si V3 no
     * llegara a correr. Lo mismo con la columna que retira V4 y la restricción
     * que agrega V6: son efectos que solo existen en la base.
     */
    @Test
    @DisplayName("cada migración destructiva dejó la base como dice su comentario")
    void eachMigrationDidWhatItClaims() {
        assertThat(tablesIn("public"))
                .as("V3 dice que elimina el andamiaje de Public User.")
                .doesNotContain("public_users", "favorites", "product_reviews");

        assertThat(migratedColumns().getOrDefault("users", Set.of()))
                .as("V4 dice que elimina la columna system_admin de users.")
                .doesNotContain("system_admin");

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE email = 'sysadmin@malva.com'", Integer.class))
                .as("V5 dice que elimina el usuario sysadmin.")
                .isZero();

        assertThat(constraintsOn("product_ingredients"))
                .as("V6 dice que agrega la unicidad de (product_id, ingredient_id), que es "
                        + "lo que convierte el doble submit de la receta en un error visible.")
                .contains("uk_product_ingredient");
    }

    // ---------- Lecturas ----------

    /**
     * Tabla -&gt; columnas, tal como las va a pedir Hibernate en runtime.
     *
     * <p>El identificador va aparte: {@code forEachSelectable} sobre el
     * persister recorre los atributos y <b>no</b> incluye la clave primaria.
     * Sin la segunda pasada, la columna {@code id} de las doce tablas quedaba
     * fuera de la comparación —y el test pasaba igual, que es justamente lo que
     * lo hacía peligroso—. Lo encontró {@link #theComparisonHasSomethingToCompare()}.
     */
    private Map<String, Set<String>> mappedColumns() {
        Map<String, Set<String>> byTable = new TreeMap<>();
        SessionFactoryImplementor sessionFactory =
                entityManagerFactory.unwrap(SessionFactoryImplementor.class);

        sessionFactory.getMappingMetamodel().forEachEntityDescriptor(persister -> {
            SelectableConsumer collect = (index, selectable) -> {
                if (selectable.isFormula()) {
                    return;
                }
                byTable.computeIfAbsent(
                                selectable.getContainingTableExpression().toLowerCase(),
                                key -> new TreeSet<>())
                        .add(selectable.getSelectionExpression().toLowerCase());
            };
            persister.forEachSelectable(collect);
            persister.getIdentifierMapping().forEachSelectable(collect);
        });

        return byTable;
    }

    /** Tabla -> columnas, tal como quedaron después de V1..V6. */
    private Map<String, Set<String>> migratedColumns() {
        Map<String, Set<String>> byTable = new TreeMap<>();
        jdbc.queryForList(
                        "SELECT table_name, column_name FROM information_schema.columns "
                                + "WHERE table_schema = 'public'")
                .forEach(row -> byTable
                        .computeIfAbsent(((String) row.get("table_name")).toLowerCase(),
                                key -> new TreeSet<>())
                        .add(((String) row.get("column_name")).toLowerCase()));
        byTable.remove("flyway_schema_history");
        return byTable;
    }

    private Set<String> tablesIn(String schema) {
        return new TreeSet<>(jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ?",
                String.class, schema));
    }

    private Set<String> constraintsOn(String table) {
        return new TreeSet<>(jdbc.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE table_schema = 'public' AND table_name = ?",
                String.class, table));
    }
}
