package com.malva_pastry_shop.backend.service.storefront;

import com.malva_pastry_shop.backend.domain.auth.User;
import com.malva_pastry_shop.backend.domain.storefront.StorefrontSection;
import com.malva_pastry_shop.backend.dto.request.StorefrontSectionRequest;
import com.malva_pastry_shop.backend.repository.ProductRepository;
import com.malva_pastry_shop.backend.repository.StorefrontSectionProductRepository;
import com.malva_pastry_shop.backend.repository.StorefrontSectionRepository;
import com.malva_pastry_shop.backend.util.ImageUrlResolver;
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
@DisplayName("StorefrontSectionService Tests")
class StorefrontSectionServiceTest {

    @Mock
    private StorefrontSectionRepository sectionRepository;

    @Mock
    private StorefrontSectionProductRepository sectionProductRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ImageUrlResolver imageUrlResolver;

    @InjectMocks
    private StorefrontSectionService sectionService;

    private StorefrontSectionRequest request;

    @BeforeEach
    void setUp() {
        request = new StorefrontSectionRequest();
        request.setName("Destacados");
        request.setDisplayOrder(1);
        request.setVisible(true);
    }

    private StorefrontSection existingSection(Long id, String name, String slug) {
        StorefrontSection section = new StorefrontSection(name);
        section.setId(id);
        section.setSlug(slug);
        return section;
    }

    @Nested
    @DisplayName("create Tests")
    class CreateTests {

        @Test
        @DisplayName("Debe crear la sección con el slug derivado del nombre")
        void create_WithValidName_DerivesSlug() {
            when(sectionRepository.findByNameIgnoreCase("Destacados")).thenReturn(Optional.empty());
            when(sectionRepository.findBySlug("destacados")).thenReturn(Optional.empty());
            when(sectionRepository.save(any(StorefrontSection.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            StorefrontSection result = sectionService.create(request);

            assertThat(result.getSlug()).isEqualTo("destacados");
            assertThat(result.getVisible()).isTrue();
        }

        @Test
        @DisplayName("Debe rechazar un nombre distinto que genera un slug ya usado")
        void create_WhenSlugCollides_ThrowsException() {
            request.setName("Otono");
            when(sectionRepository.findByNameIgnoreCase("Otono")).thenReturn(Optional.empty());
            when(sectionRepository.findBySlug("otono"))
                    .thenReturn(Optional.of(existingSection(1L, "Otoño", "otono")));

            assertThatThrownBy(() -> sectionService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("genera el mismo enlace ('otono')")
                    .hasMessageContaining("Otoño");

            verify(sectionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe rechazar un slug ocupado por una sección en la papelera")
        void create_WhenSlugHeldByDeletedSection_ThrowsExplainingTrash() {
            request.setName("Otono");
            StorefrontSection deleted = existingSection(1L, "Otoño", "otono");
            deleted.softDelete(new User());

            when(sectionRepository.findByNameIgnoreCase("Otono")).thenReturn(Optional.empty());
            when(sectionRepository.findBySlug("otono")).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> sectionService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("está en la papelera");

            verify(sectionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe rechazar un nombre sin caracteres alfanuméricos")
        void create_WhenNameHasNoAlphanumerics_ThrowsException() {
            request.setName("***");
            when(sectionRepository.findByNameIgnoreCase("***")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sectionService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no genera un enlace válido");

            verify(sectionRepository, never()).findBySlug(anyString());
            verify(sectionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update Tests")
    class UpdateTests {

        @Test
        @DisplayName("No debe considerar conflicto el slug de la propia sección")
        void update_WhenSlugBelongsToSameSection_Succeeds() {
            StorefrontSection section = existingSection(1L, "Destacados", "destacados");

            when(sectionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(section));
            when(sectionRepository.findByNameIgnoreCase("Destacados")).thenReturn(Optional.of(section));
            when(sectionRepository.findBySlug("destacados")).thenReturn(Optional.of(section));
            when(sectionRepository.save(any(StorefrontSection.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            StorefrontSection result = sectionService.update(1L, request);

            assertThat(result.getSlug()).isEqualTo("destacados");
            assertThat(result.getDisplayOrder()).isEqualTo(1);
        }
    }
}
