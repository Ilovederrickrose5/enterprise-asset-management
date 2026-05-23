package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.entity.User;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssetServicePermissionTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AssetService assetService;

    private User adminUser;
    private User leaderUser;
    private User managerUser;
    private User regularUser;
    private Asset asset;
    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 初始化用户
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRole("admin");

        leaderUser = new User();
        leaderUser.setId(2L);
        leaderUser.setUsername("leader");
        leaderUser.setRole("leader");

        managerUser = new User();
        managerUser.setId(3L);
        managerUser.setUsername("manager");
        managerUser.setDeptId(1L);
        managerUser.setRole("manager");

        regularUser = new User();
        regularUser.setId(4L);
        regularUser.setUsername("user");
        regularUser.setRole("user");

        // 初始化资产
        asset = new Asset();
        asset.setId(1L);
        asset.setAssetName("测试资产");
        asset.setDeptId(1L);
        asset.setUserId(4L);
        asset.setStatus("in_stock");
        asset.setUseStatus("idle");

        // 模拟SecurityContext
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    @AfterEach
    void tearDown() {
        if (mockedSecurityContextHolder != null) {
            mockedSecurityContextHolder.close();
        }
    }

    private void setAuthentication(User user) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getUsername());
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    @Test
    void testAdminCanCreateAsset() {
        setAuthentication(adminUser);
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        Asset result = assetService.createAsset(asset);
        assertNotNull(result);
        verify(assetRepository, times(1)).save(asset);
    }

    @Test
    void testLeaderCannotCreateAsset() {
        setAuthentication(leaderUser);

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            assetService.createAsset(asset);
        });
        assertEquals("领导角色无权限创建资产", exception.getMessage());
    }

    @Test
    void testManagerCanCreateAssetInOwnDepartment() {
        setAuthentication(managerUser);
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        Asset result = assetService.createAsset(asset);
        assertNotNull(result);
        assertEquals(1L, result.getDeptId()); // 确保部门ID被设置为管理员所在部门
        verify(assetRepository, times(1)).save(asset);
    }

    @Test
    void testAdminCanUpdateAsset() {
        setAuthentication(adminUser);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        Asset result = assetService.updateAsset(1L, asset);
        assertNotNull(result);
        verify(assetRepository, times(1)).save(asset);
    }

    @Test
    void testLeaderCannotUpdateAsset() {
        setAuthentication(leaderUser);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            assetService.updateAsset(1L, asset);
        });
        assertEquals("领导角色无权限更新资产", exception.getMessage());
    }

    @Test
    void testManagerCanUpdateAssetInOwnDepartment() {
        setAuthentication(managerUser);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        Asset result = assetService.updateAsset(1L, asset);
        assertNotNull(result);
        verify(assetRepository, times(1)).save(asset);
    }

    @Test
    void testManagerCannotUpdateAssetInOtherDepartment() {
        setAuthentication(managerUser);
        Asset otherDepartmentAsset = new Asset();
        otherDepartmentAsset.setId(2L);
        otherDepartmentAsset.setDeptId(2L); // 不同部门
        when(assetRepository.findById(2L)).thenReturn(Optional.of(otherDepartmentAsset));

        Asset result = assetService.updateAsset(2L, otherDepartmentAsset);
        assertNull(result);
    }

    @Test
    void testAdminCanDeleteAsset() {
        setAuthentication(adminUser);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        doNothing().when(assetRepository).deleteById(1L);

        boolean result = assetService.deleteAsset(1L);
        assertTrue(result);
        verify(assetRepository, times(1)).deleteById(1L);
    }

    @Test
    void testLeaderCannotDeleteAsset() {
        setAuthentication(leaderUser);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            assetService.deleteAsset(1L);
        });
        assertEquals("领导角色无权限删除资产", exception.getMessage());
    }

    @Test
    void testManagerCanDeleteAssetInOwnDepartment() {
        setAuthentication(managerUser);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        doNothing().when(assetRepository).deleteById(1L);

        boolean result = assetService.deleteAsset(1L);
        assertTrue(result);
        verify(assetRepository, times(1)).deleteById(1L);
    }

    @Test
    void testManagerCannotDeleteAssetInOtherDepartment() {
        setAuthentication(managerUser);
        Asset otherDepartmentAsset = new Asset();
        otherDepartmentAsset.setId(2L);
        otherDepartmentAsset.setDeptId(2L); // 不同部门
        when(assetRepository.findById(2L)).thenReturn(Optional.of(otherDepartmentAsset));

        boolean result = assetService.deleteAsset(2L);
        assertFalse(result);
    }

    @Test
    void testAdminCanUpdateAssetStatus() {
        setAuthentication(adminUser);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        Asset result = assetService.updateAssetStatus(1L, "using");
        assertNotNull(result);
        verify(assetRepository, times(1)).save(asset);
    }

    @Test
    void testLeaderCannotUpdateAssetStatus() {
        setAuthentication(leaderUser);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            assetService.updateAssetStatus(1L, "using");
        });
        assertEquals("领导角色无权限更新资产状态", exception.getMessage());
    }

    @Test
    void testManagerCanUpdateAssetStatusInOwnDepartment() {
        setAuthentication(managerUser);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        Asset result = assetService.updateAssetStatus(1L, "using");
        assertNotNull(result);
        verify(assetRepository, times(1)).save(asset);
    }

    @Test
    void testManagerCannotUpdateAssetStatusInOtherDepartment() {
        setAuthentication(managerUser);
        Asset otherDepartmentAsset = new Asset();
        otherDepartmentAsset.setId(2L);
        otherDepartmentAsset.setDeptId(2L); // 不同部门
        when(assetRepository.findById(2L)).thenReturn(Optional.of(otherDepartmentAsset));

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            assetService.updateAssetStatus(2L, "using");
        });
        assertEquals("部门资产管理员只能更新本部门资产状态", exception.getMessage());
    }

    @Test
    void testAdminCanUpdateAssetUseStatus() {
        setAuthentication(adminUser);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        Asset result = assetService.updateAssetUseStatus(1L, "using");
        assertNotNull(result);
        verify(assetRepository, times(1)).save(asset);
    }

    @Test
    void testLeaderCannotUpdateAssetUseStatus() {
        setAuthentication(leaderUser);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            assetService.updateAssetUseStatus(1L, "using");
        });
        assertEquals("领导角色无权限更新资产使用状态", exception.getMessage());
    }

    @Test
    void testManagerCanUpdateAssetUseStatusInOwnDepartment() {
        setAuthentication(managerUser);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        Asset result = assetService.updateAssetUseStatus(1L, "using");
        assertNotNull(result);
        verify(assetRepository, times(1)).save(asset);
    }

    @Test
    void testManagerCannotUpdateAssetUseStatusInOtherDepartment() {
        setAuthentication(managerUser);
        Asset otherDepartmentAsset = new Asset();
        otherDepartmentAsset.setId(2L);
        otherDepartmentAsset.setDeptId(2L); // 不同部门
        when(assetRepository.findById(2L)).thenReturn(Optional.of(otherDepartmentAsset));

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            assetService.updateAssetUseStatus(2L, "using");
        });
        assertEquals("部门资产管理员只能更新本部门资产使用状态", exception.getMessage());
    }
}
