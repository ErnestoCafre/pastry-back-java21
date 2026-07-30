package com.malva_pastry_shop.backend.service.storefront;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.storefront.Tag;
import com.malva_pastry_shop.backend.dto.request.TagRequest;
import com.malva_pastry_shop.backend.repository.ProductTagRepository;
import com.malva_pastry_shop.backend.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagService Tests")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ProductTagRepository productTagRepository;

    @InjectMocks
    private TagService tagService;

    private TagRequest request;

    @BeforeEach
    void setUp() {
        request = new TagRequest();
        request.setName("Sin Gluten");
        request.setDescription("Apto celiacos");
    }

    private Tag existingTag(Long id, String name, String slug) {
        Tag tag = new Tag(name);
        tag.setId(id);
        tag.setSlug(slug);
        return tag;
    }

    @Nested
    @DisplayName("create Tests")
    class CreateTests {

        @Test
        @DisplayName("Debe crear el tag con el slug derivado del nombre")
        void create_WithValidName_DerivesSlug() {
            when(tagRepository.findByNameIgnoreCase("Sin Gluten")).thenReturn(Optional.empty());
            when(tagRepository.findBySlug("sin-gluten")).thenReturn(Optional.empty());
            when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Tag result = tagService.create(request);

            assertThat(result.getName()).isEqualTo("Sin Gluten");
            assertThat(result.getSlug()).isEqualTo("sin-gluten");
        }

        @Test
        @DisplayName("Debe rechazar un nombre distinto que genera un slug ya usado")
        void create_WhenSlugCollidesWithActiveTag_ThrowsException() {
            request.setName("sin-gluten");
            when(tagRepository.findByNameIgnoreCase("sin-gluten")).thenReturn(Optional.empty());
            when(tagRepository.findBySlug("sin-gluten"))
                    .thenReturn(Optional.of(existingTag(1L, "Sin Gluten", "sin-gluten")));

            assertThatThrownBy(() -> tagService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("genera el mismo enlace ('sin-gluten')")
                    .hasMessageContaining("Sin Gluten");

            verify(tagRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe rechazar un slug ocupado por un tag en la papelera, explicando dónde está")
        void create_WhenSlugHeldByDeletedTag_ThrowsExplainingTrash() {
            request.setName("Citricos");
            Tag deleted = existingTag(1L, "Cítricos", "citricos");
            deleted.softDelete(new User());

            when(tagRepository.findByNameIgnoreCase("Citricos")).thenReturn(Optional.empty());
            when(tagRepository.findBySlug("citricos")).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> tagService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("está en la papelera");

            verify(tagRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe rechazar un nombre sin caracteres alfanuméricos")
        void create_WhenNameHasNoAlphanumerics_ThrowsException() {
            request.setName("***");
            when(tagRepository.findByNameIgnoreCase("***")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tagService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no genera un enlace válido");

            verify(tagRepository, never()).findBySlug(anyString());
            verify(tagRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update Tests")
    class UpdateTests {

        @Test
        @DisplayName("No debe considerar conflicto el slug del propio tag")
        void update_WhenSlugBelongsToSameTag_Succeeds() {
            Tag tag = existingTag(1L, "Sin Gluten", "sin-gluten");

            when(tagRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tag));
            when(tagRepository.findByNameIgnoreCase("Sin Gluten")).thenReturn(Optional.of(tag));
            when(tagRepository.findBySlug("sin-gluten")).thenReturn(Optional.of(tag));
            when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Tag result = tagService.update(1L, request);

            assertThat(result.getSlug()).isEqualTo("sin-gluten");
            assertThat(result.getDescription()).isEqualTo("Apto celiacos");
        }

        @Test
        @DisplayName("Debe rechazar un renombre cuyo slug pertenece a otro tag")
        void update_WhenSlugBelongsToAnotherTag_ThrowsException() {
            Tag tag = existingTag(1L, "Vegano", "vegano");
            request.setName("Sin-Gluten");

            when(tagRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tag));
            when(tagRepository.findByNameIgnoreCase("Sin-Gluten")).thenReturn(Optional.empty());
            when(tagRepository.findBySlug("sin-gluten"))
                    .thenReturn(Optional.of(existingTag(2L, "Sin Gluten", "sin-gluten")));

            assertThatThrownBy(() -> tagService.update(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("genera el mismo enlace ('sin-gluten')");

            verify(tagRepository, never()).save(any());
        }
    }
}
