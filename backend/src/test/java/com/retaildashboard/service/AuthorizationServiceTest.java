package com.retaildashboard.service;

import com.retaildashboard.domain.DataType;
import com.retaildashboard.domain.Role;
import com.retaildashboard.domain.User;
import com.retaildashboard.exception.AccessDeniedException;
import com.retaildashboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * AuthorizationService 단위 테스트.
 * 역할별 데이터 접근 권한 매트릭스와 브랜드별 접근 제한을 검증합니다.
 *
 * Requirements: 3.2, 3.3, 3.4, 3.5, 3.6
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private User ceoUser;
    private User executiveUser;
    private User financeUser;
    private User marketingUser;
    private User productUser;

    @BeforeEach
    void setUp() {
        ceoUser = buildUser(Role.CEO, null);
        executiveUser = buildUser(Role.EXECUTIVE, "BrandA");
        financeUser = buildUser(Role.FINANCE, "BrandA");
        marketingUser = buildUser(Role.MARKETING, "BrandB");
        productUser = buildUser(Role.PRODUCT, "BrandC");
    }

    private User buildUser(Role role, String assignedBrand) {
        return User.builder()
                .id(UUID.randomUUID())
                .username(role.name().toLowerCase() + "_user")
                .passwordHash("hash")
                .email(role.name().toLowerCase() + "@test.com")
                .role(role)
                .assignedBrand(assignedBrand)
                .isActive(true)
                .failedLoginCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("역할별 데이터 접근 권한 테스트")
    class RoleAccessTests {

        @Test
        @DisplayName("CEO는 모든 데이터 유형에 접근 가능")
        void ceo_shouldAccessAllDataTypes() {
            for (DataType dataType : DataType.values()) {
                assertThat(authorizationService.hasAccess(ceoUser, dataType))
                        .as("CEO should access " + dataType)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Executive는 모든 데이터 유형에 접근 가능")
        void executive_shouldAccessAllDataTypes() {
            for (DataType dataType : DataType.values()) {
                assertThat(authorizationService.hasAccess(executiveUser, dataType))
                        .as("Executive should access " + dataType)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Finance는 비용 데이터에 접근 가능 (Req 3.2)")
        void finance_shouldAccessCostData() {
            assertThat(authorizationService.hasAccess(financeUser, DataType.COSTS)).isTrue();
        }

        @Test
        @DisplayName("Finance는 모든 데이터 유형에 접근 가능")
        void finance_shouldAccessAllDataTypes() {
            for (DataType dataType : DataType.values()) {
                assertThat(authorizationService.hasAccess(financeUser, dataType))
                        .as("Finance should access " + dataType)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Marketing은 비용 데이터에 접근 불가 (Req 3.3)")
        void marketing_shouldNotAccessCostData() {
            assertThat(authorizationService.hasAccess(marketingUser, DataType.COSTS)).isFalse();
        }

        @Test
        @DisplayName("Marketing은 광고비 데이터에 접근 가능 (Req 3.5)")
        void marketing_shouldAccessAdvertisingData() {
            assertThat(authorizationService.hasAccess(marketingUser, DataType.ADVERTISING)).isTrue();
        }

        @Test
        @DisplayName("Marketing은 매출 데이터에 접근 가능 (Req 3.4)")
        void marketing_shouldAccessSalesData() {
            assertThat(authorizationService.hasAccess(marketingUser, DataType.SALES)).isTrue();
        }

        @Test
        @DisplayName("Product는 비용 데이터에 접근 불가 (Req 3.3)")
        void product_shouldNotAccessCostData() {
            assertThat(authorizationService.hasAccess(productUser, DataType.COSTS)).isFalse();
        }

        @Test
        @DisplayName("Product는 광고비 데이터에 접근 불가")
        void product_shouldNotAccessAdvertisingData() {
            assertThat(authorizationService.hasAccess(productUser, DataType.ADVERTISING)).isFalse();
        }

        @Test
        @DisplayName("Product는 매출/판매량 데이터에 접근 가능 (Req 3.4)")
        void product_shouldAccessSalesData() {
            assertThat(authorizationService.hasAccess(productUser, DataType.SALES)).isTrue();
        }

        @Test
        @DisplayName("전체 역할은 매출 데이터에 접근 가능 (Req 3.4)")
        void allRoles_shouldAccessSalesData() {
            assertThat(authorizationService.hasAccess(ceoUser, DataType.SALES)).isTrue();
            assertThat(authorizationService.hasAccess(executiveUser, DataType.SALES)).isTrue();
            assertThat(authorizationService.hasAccess(financeUser, DataType.SALES)).isTrue();
            assertThat(authorizationService.hasAccess(marketingUser, DataType.SALES)).isTrue();
            assertThat(authorizationService.hasAccess(productUser, DataType.SALES)).isTrue();
        }

        @Test
        @DisplayName("전체 역할은 재고 데이터에 접근 가능")
        void allRoles_shouldAccessInventoryData() {
            assertThat(authorizationService.hasAccess(ceoUser, DataType.INVENTORY)).isTrue();
            assertThat(authorizationService.hasAccess(executiveUser, DataType.INVENTORY)).isTrue();
            assertThat(authorizationService.hasAccess(financeUser, DataType.INVENTORY)).isTrue();
            assertThat(authorizationService.hasAccess(marketingUser, DataType.INVENTORY)).isTrue();
            assertThat(authorizationService.hasAccess(productUser, DataType.INVENTORY)).isTrue();
        }

        @Test
        @DisplayName("null 사용자는 접근 불가")
        void nullUser_shouldDenyAccess() {
            assertThat(authorizationService.hasAccess(null, DataType.SALES)).isFalse();
        }

        @Test
        @DisplayName("null 데이터 유형은 접근 불가")
        void nullDataType_shouldDenyAccess() {
            assertThat(authorizationService.hasAccess(ceoUser, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("checkAccess 예외 테스트")
    class CheckAccessTests {

        @Test
        @DisplayName("접근 허용 시 예외 없음")
        void checkAccess_whenAllowed_shouldNotThrow() {
            authorizationService.checkAccess(financeUser, DataType.COSTS);
            // 예외 없이 통과
        }

        @Test
        @DisplayName("접근 거부 시 AccessDeniedException 발생")
        void checkAccess_whenDenied_shouldThrowAccessDeniedException() {
            assertThatThrownBy(() -> authorizationService.checkAccess(marketingUser, DataType.COSTS))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("MARKETING")
                    .hasMessageContaining("COSTS");
        }
    }

    @Nested
    @DisplayName("브랜드별 접근 제한 테스트 (Req 3.6)")
    class BrandAccessTests {

        @Test
        @DisplayName("CEO는 모든 브랜드에 접근 가능")
        void ceo_shouldAccessAllBrands() {
            Set<String> brands = authorizationService.getAccessibleBrands(ceoUser);
            assertThat(brands).isNull(); // null = 모든 브랜드 접근 가능
        }

        @Test
        @DisplayName("CEO는 어떤 브랜드든 접근 가능")
        void ceo_hasBrandAccess_shouldReturnTrue() {
            assertThat(authorizationService.hasBrandAccess(ceoUser, "AnyBrand")).isTrue();
        }

        @Test
        @DisplayName("assigned_brand가 있는 사용자는 해당 브랜드만 접근 가능")
        void userWithAssignedBrand_shouldOnlyAccessAssignedBrand() {
            Set<String> brands = authorizationService.getAccessibleBrands(marketingUser);
            assertThat(brands).containsExactly("BrandB");
        }

        @Test
        @DisplayName("assigned_brand가 있는 사용자는 다른 브랜드 접근 불가")
        void userWithAssignedBrand_shouldNotAccessOtherBrand() {
            assertThat(authorizationService.hasBrandAccess(marketingUser, "BrandA")).isFalse();
        }

        @Test
        @DisplayName("assigned_brand가 있는 사용자는 자신의 브랜드 접근 가능")
        void userWithAssignedBrand_shouldAccessOwnBrand() {
            assertThat(authorizationService.hasBrandAccess(marketingUser, "BrandB")).isTrue();
        }

        @Test
        @DisplayName("assigned_brand가 없는 사용자는 모든 브랜드 접근 가능")
        void userWithoutAssignedBrand_shouldAccessAllBrands() {
            User noAssignedBrand = buildUser(Role.FINANCE, null);
            Set<String> brands = authorizationService.getAccessibleBrands(noAssignedBrand);
            assertThat(brands).isNull();
        }

        @Test
        @DisplayName("assigned_brand가 빈 문자열인 사용자는 모든 브랜드 접근 가능")
        void userWithBlankAssignedBrand_shouldAccessAllBrands() {
            User blankBrand = buildUser(Role.FINANCE, "  ");
            Set<String> brands = authorizationService.getAccessibleBrands(blankBrand);
            assertThat(brands).isNull();
        }

        @Test
        @DisplayName("null 사용자의 브랜드 접근은 빈 집합 반환")
        void nullUser_shouldReturnEmptyBrands() {
            Set<String> brands = authorizationService.getAccessibleBrands(null);
            assertThat(brands).isEmpty();
        }

        @Test
        @DisplayName("null 사용자의 브랜드 접근 확인은 false 반환")
        void nullUser_hasBrandAccess_shouldReturnFalse() {
            assertThat(authorizationService.hasBrandAccess(null, "BrandA")).isFalse();
        }

        @Test
        @DisplayName("null 브랜드 접근 확인은 false 반환")
        void nullBrand_hasBrandAccess_shouldReturnFalse() {
            assertThat(authorizationService.hasBrandAccess(financeUser, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("checkBrandAccess 예외 테스트")
    class CheckBrandAccessTests {

        @Test
        @DisplayName("브랜드 접근 허용 시 예외 없음")
        void checkBrandAccess_whenAllowed_shouldNotThrow() {
            authorizationService.checkBrandAccess(marketingUser, "BrandB");
            // 예외 없이 통과
        }

        @Test
        @DisplayName("브랜드 접근 거부 시 AccessDeniedException 발생")
        void checkBrandAccess_whenDenied_shouldThrowAccessDeniedException() {
            assertThatThrownBy(() -> authorizationService.checkBrandAccess(marketingUser, "BrandA"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("BrandA");
        }
    }

    @Nested
    @DisplayName("getUserRole 테스트")
    class GetUserRoleTests {

        @Test
        @DisplayName("존재하는 사용자의 역할 반환")
        void getUserRole_withExistingUser_shouldReturnRole() {
            when(userRepository.findById(financeUser.getId())).thenReturn(Optional.of(financeUser));

            Role role = authorizationService.getUserRole(financeUser.getId());

            assertThat(role).isEqualTo(Role.FINANCE);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 예외 발생")
        void getUserRole_withNonExistentUser_shouldThrow() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authorizationService.getUserRole(unknownId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }
}
