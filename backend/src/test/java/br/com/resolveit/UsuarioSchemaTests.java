package br.com.resolveit;

import br.com.resolveit.model.Usuario;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioSchemaTests {
    @Test
    void tipoDaColunaPostgresNaoIncluiClausulaDefault() {
        // Inspeciona o SQL do PostgreSQL sem abrir conexão nem alterar dados.
        try (var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .build()) {
            var metadata = new MetadataSources(registry).addAnnotatedClass(Usuario.class).buildMetadata();
            var perfil = metadata.getEntityBinding(Usuario.class.getName()).getTable()
                    .getColumn(new Column("perfil"));

            // ALTER COLUMN TYPE recebe apenas o tipo; DEFAULT é metadado separado.
            assertThat(perfil.getSqlType(metadata)).isEqualTo("varchar(20)");
            assertThat(perfil.getDefaultValue()).isEqualTo("'USUARIO'");
            assertThat(perfil.isNullable()).isFalse();
        }
    }
}
