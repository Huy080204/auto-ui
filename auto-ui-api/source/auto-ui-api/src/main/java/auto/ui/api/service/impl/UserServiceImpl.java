package auto.ui.api.service.impl;

import auto.ui.api.config.SecurityConstant;
import auto.ui.api.constant.AIConstant;
import auto.ui.api.dto.ErrorCode;
import auto.ui.api.exception.BadRequestException;
import auto.ui.api.jwt.BaseJwt;
import auto.ui.api.model.Account;
import auto.ui.api.model.Group;
import auto.ui.api.repository.AccountRepository;
import auto.ui.api.repository.GroupRepository;
import auto.ui.api.service.mfa.TotpManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service(value = "userService")
@Slf4j
public class UserServiceImpl implements UserDetailsService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TotpManager totpManager;

    @Value("${mfa}")
    private Boolean isMfaEnable;

    @Override
    public UserDetails loadUserByUsername(String userId) {
        Account user = accountRepository.findAccountByUsername(userId);
        if (user == null) {
            log.error("Invalid username or password.");
            throw new UsernameNotFoundException("Invalid username or password.");
        }
        boolean enabled = true;
        if (user.getStatus() != AIConstant.STATUS_ACTIVE) {
            log.error("User had been locked");
            enabled = false;
        }
        Set<GrantedAuthority> grantedAuthorities = getAccountPermission(user);

        return new User(user.getUsername(), user.getPassword(), enabled, true, true, true, grantedAuthorities);
    }

    private Set<GrantedAuthority> getAccountPermission(Account user) {
        List<String> roles = new ArrayList<>();
        user.getGroup().getPermissions().stream().filter(f -> f.getPCode() != null).forEach(pName -> roles.add(pName.getPCode()));
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())).collect(Collectors.toSet());
    }

    public OAuth2AccessToken getAccessTokenForPasswordOtp(ClientDetails client, String username, String password, String otp, String grantType, AuthorizationServerTokenServices tokenServices) {
        if (isMfaEnable && StringUtils.isBlank(otp)) {
            throw new BadRequestException("otp cannot be null", ErrorCode.AUTH_GRANT_TYPE_PASSWORD_MFA_ERROR_OTP_BLANK);
        }

        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("grant_type", grantType);
        String clientId = client.getClientId();
        boolean approved = true;
        Set<String> responseTypes = new HashSet<>();
        Map<String, Serializable> extensionProperties = new HashMap<>();
        UserDetails userDetails = loadUserForPasswordOtp(username, password, otp);

        OAuth2Request oAuth2Request = new OAuth2Request(requestParameters, clientId,
                userDetails.getAuthorities(), approved, client.getScope(),
                client.getResourceIds(), null, responseTypes, extensionProperties);
        User userPrincipal = new User(userDetails.getUsername(), userDetails.getPassword(), userDetails.isEnabled(), userDetails.isAccountNonExpired(), userDetails.isCredentialsNonExpired(), userDetails.isAccountNonLocked(), userDetails.getAuthorities());
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userPrincipal, null, userDetails.getAuthorities());
        OAuth2Authentication auth = new OAuth2Authentication(oAuth2Request, authenticationToken);
        return tokenServices.createAccessToken(auth);
    }

    private UserDetails loadUserForPasswordOtp(String username, String password, String otp) {
        Account user = accountRepository.findFirstByUsernameOrEmailOrPhoneAndStatus(username, AIConstant.STATUS_ACTIVE)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password!"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UsernameNotFoundException("Invalid username or password!");
        }

        if (isMfaEnable) {
            boolean isVerified = user.getSecretKey() != null && totpManager.verifyCode(otp, user.getSecretKey());
            if (!isVerified) {
                throw new BadRequestException("Verified Fail", ErrorCode.ACCOUNT_ERROR_OPT_INVALID);
            }
            if (!Boolean.TRUE.equals(user.getIsMfa())) {
                user.setIsMfa(true);
                accountRepository.save(user);
            }
        }
        Set<GrantedAuthority> grantedAuthorities = getAccountPermission(user);
        return new User(user.getUsername(), user.getPassword(), true, true, true, true, grantedAuthorities);
    }

    public OAuth2AccessToken getAccessTokenForMultipleTenancies(ClientDetails client, TokenRequest tokenRequest, String username, String password, String tenant, AuthorizationServerTokenServices tokenServices) throws GeneralSecurityException, IOException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("grantType", SecurityConstant.GRANT_TYPE_PASSWORD);
        requestParameters.put("tenantId", tokenRequest.getRequestParameters().get("tenantId"));

        String clientId = client.getClientId();
        boolean approved = true;
        Set<String> responseTypes = new HashSet<>();
        responseTypes.add("code");
        Map<String, Serializable> extensionProperties = new HashMap<>();

        UserDetails userDetails = loadUserByUsername(username);
        OAuth2Request oAuth2Request = new OAuth2Request(requestParameters, clientId,
                userDetails.getAuthorities(), approved, client.getScope(),
                client.getResourceIds(), null, responseTypes, extensionProperties);
        User userPrincipal = new User(userDetails.getUsername(), userDetails.getPassword(), userDetails.isEnabled(), userDetails.isAccountNonExpired(), userDetails.isCredentialsNonExpired(), userDetails.isAccountNonLocked(), userDetails.getAuthorities());
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userPrincipal, null, userDetails.getAuthorities());
        OAuth2Authentication auth = new OAuth2Authentication(oAuth2Request, authenticationToken);
        return tokenServices.createAccessToken(auth);
    }

    public String getAttributeFromToken(String attribute) {
        Map<String, Object> map = getAttributeFromToken();
        if (map != null) {
            return String.valueOf(map.get(attribute));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAttributeFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            OAuth2AuthenticationDetails oauthDetails =
                    (OAuth2AuthenticationDetails) authentication.getDetails();
            if (oauthDetails != null) {
                return (Map<String, Object>) oauthDetails.getDecodedDetails();
            }
        }
        return null;
    }

    public BaseJwt getAddInfoFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            OAuth2AuthenticationDetails oauthDetails =
                    (OAuth2AuthenticationDetails) authentication.getDetails();
            if (oauthDetails != null) {
                Map<String, Object> map = (Map<String, Object>) oauthDetails.getDecodedDetails();
                String encodedData = (String) map.get("additional_info");
                //idStr -> json
                if (encodedData != null && !encodedData.isEmpty()) {
                    return BaseJwt.decode(encodedData);
                }
                return null;
            }
        }
        return null;
    }

    public String getCurrentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            OAuth2AuthenticationDetails oauthDetails =
                    (OAuth2AuthenticationDetails) authentication.getDetails();
            if (oauthDetails != null) {
                return oauthDetails.getTokenValue();
            }
        }
        return null;
    }
}
