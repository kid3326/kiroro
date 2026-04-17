package com.retaildashboard.service;

import com.retaildashboard.domain.DataType;
import com.retaildashboard.domain.Role;
import com.retaildashboard.domain.User;
import com.retaildashboard.exception.AccessDeniedException;
import com.retaildashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 권한 관리 서비스.
 * 역할별 데이터 접근 권한 매트릭스와 브랜드별 데이터 접근 제한을 구현합니다.
 *
 * 접근 권한 매트릭스:
 * | DataType     | CEO | Executive | Finance | Marketing | Product |
 * |-------------|-----|-----------|---------|-----------|---------|
 * | SALES       |  ✓  |     ✓     |    ✓    |     ✓     |    ✓    |
 * | COSTS       |  ✓  |     ✓     |    ✓    |     ✗     |    ✗    |
 * | ADVERTISING |  ✓  |     ✓     |    ✓    |     ✓     |    ✗    |
 * | INVENTORY   |  ✓  |     ✓     |    ✓    |     ✓     |    ✓    |
 * | PRODUCT     |  ✓  |     ✓     |    ✓    |     ✓     |    ✓    |
 *
 * Requirements: 3.2, 3.3, 3.4, 3.5, 3.6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final UserRepository userRepository;

    /**
     * 역할별 데이터 접근 권한 매트릭스.
     * CEO는 모든 데이터에 접근 가능합니다.
     */
    private static final Map<Role, Set<DataType>> ROLE_ACCESS_MATRIX = Map.of(
            Role.CEO, EnumSet.allOf(DataType.class),
            Role.EXECUTIVE, EnumSet.allOf(DataType.class),
            Role.FINANCE, EnumSet.of(DataType.SALES, DataType.COSTS, DataType.ADVERTISING, DataType.INVENTORY, DataType.PRODUCT),
            Role.MARKETING, EnumSet.of(DataType.SALES, DataType.ADVERTISING, DataType.INVENTORY, DataType.PRODUCT),
            Role.PRODUCT, EnumSet.of(DataType.SALES, DataType.INVENTORY, DataType.PRODUCT)
    );

    /**
     * 사용자가 특정 데이터 유형에 접근할 수 있는지 확인합니다.
     *
     * @param user     사용자
     * @param dataType 데이터 유형
     * @return 접근 허용 여부
     */
    public boolean hasAccess(User user, DataType dataType) {
        if (user == null || dataType == null) {
            return false;
        }

        Role role = user.getRole();
        Set<DataType> allowedTypes = ROLE_ACCESS_MATRIX.get(role);
        if (allowedTypes == null) {
            return false;
        }

        return allowedTypes.contains(dataType);
    }

    /**
     * 사용자가 특정 데이터 유형에 접근할 수 있는지 확인하고,
     * 접근이 거부되면 예외를 발생시킵니다.
     *
     * @param user     사용자
     * @param dataType 데이터 유형
     * @throws AccessDeniedException 접근이 거부된 경우
     */
    public void checkAccess(User user, DataType dataType) {
        if (!hasAccess(user, dataType)) {
            log.warn("데이터 접근 거부: userId={}, role={}, dataType={}",
                    user.getId(), user.getRole(), dataType);
            throw new AccessDeniedException(
                    String.format("역할 '%s'은(는) '%s' 데이터에 접근할 수 없습니다", user.getRole(), dataType));
        }
    }

    /**
     * 사용자가 접근할 수 있는 브랜드 목록을 반환합니다.
     * CEO는 모든 브랜드에 접근 가능합니다.
     * 다른 역할은 assigned_brand에 해당하는 브랜드만 접근 가능합니다.
     *
     * @param user 사용자
     * @return 접근 가능한 브랜드 목록 (null이면 모든 브랜드 접근 가능)
     */
    public Set<String> getAccessibleBrands(User user) {
        if (user == null) {
            return Set.of();
        }

        // CEO는 모든 브랜드에 접근 가능
        if (user.getRole() == Role.CEO) {
            return null; // null은 모든 브랜드 접근 가능을 의미
        }

        // assigned_brand가 없으면 모든 브랜드 접근 가능
        String assignedBrand = user.getAssignedBrand();
        if (assignedBrand == null || assignedBrand.isBlank()) {
            return null;
        }

        return Set.of(assignedBrand);
    }

    /**
     * 사용자가 특정 브랜드의 데이터에 접근할 수 있는지 확인합니다.
     *
     * @param user  사용자
     * @param brand 브랜드명
     * @return 접근 허용 여부
     */
    public boolean hasBrandAccess(User user, String brand) {
        if (user == null || brand == null) {
            return false;
        }

        Set<String> accessibleBrands = getAccessibleBrands(user);

        // null이면 모든 브랜드 접근 가능
        if (accessibleBrands == null) {
            return true;
        }

        return accessibleBrands.contains(brand);
    }

    /**
     * 사용자가 특정 브랜드의 데이터에 접근할 수 있는지 확인하고,
     * 접근이 거부되면 예외를 발생시킵니다.
     *
     * @param user  사용자
     * @param brand 브랜드명
     * @throws AccessDeniedException 접근이 거부된 경우
     */
    public void checkBrandAccess(User user, String brand) {
        if (!hasBrandAccess(user, brand)) {
            log.warn("브랜드 접근 거부: userId={}, assignedBrand={}, requestedBrand={}",
                    user.getId(), user.getAssignedBrand(), brand);
            throw new AccessDeniedException(
                    String.format("사용자는 브랜드 '%s'의 데이터에 접근할 수 없습니다", brand));
        }
    }

    /**
     * 사용자의 역할을 반환합니다.
     *
     * @param userId 사용자 ID
     * @return 사용자 역할
     */
    public Role getUserRole(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getRole)
                .orElseThrow(() -> new AccessDeniedException("사용자를 찾을 수 없습니다: " + userId));
    }
}
