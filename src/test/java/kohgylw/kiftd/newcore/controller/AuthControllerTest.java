package kohgylw.kiftd.newcore.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.newcore.domain.OperationResult;
import kohgylw.kiftd.newcore.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Test
    void testGetPublicKey_Success() {
        String publicKeyJson = "{\"publicKey\":\"testKey\",\"time\":123456}";
        when(authService.getPublicKeyJson()).thenReturn(publicKeyJson);

        String result = authController.getPublicKey();

        assertEquals(publicKeyJson, result);
        verify(authService, times(1)).getPublicKeyJson();
    }

    @Test
    void testGetPublicKey_Empty() {
        when(authService.getPublicKeyJson()).thenReturn("");

        String result = authController.getPublicKey();

        assertEquals("", result);
        verify(authService, times(1)).getPublicKeyJson();
    }

    @Test
    void testDoLogin_Success() {
        OperationResult successResult = OperationResult.success("permitlogin");
        when(authService.login(any(HttpServletRequest.class), any(HttpSession.class))).thenReturn(successResult);

        String result = authController.doLogin(request, session);

        assertEquals("permitlogin", result);
        verify(authService, times(1)).login(request, session);
    }

    @Test
    void testDoLogin_Failure() {
        OperationResult failureResult = OperationResult.failure("accountnotfound");
        when(authService.login(any(HttpServletRequest.class), any(HttpSession.class))).thenReturn(failureResult);

        String result = authController.doLogin(request, session);

        assertEquals("accountnotfound", result);
        verify(authService, times(1)).login(request, session);
    }

    @Test
    void testDoLogin_PasswordError() {
        OperationResult failureResult = OperationResult.failure("accountpwderror");
        when(authService.login(any(HttpServletRequest.class), any(HttpSession.class))).thenReturn(failureResult);

        String result = authController.doLogin(request, session);

        assertEquals("accountpwderror", result);
        verify(authService, times(1)).login(request, session);
    }

    @Test
    void testGetNewVerCode_Success() {
        doNothing().when(authService).getVerificationCode(any(HttpServletRequest.class), any(HttpServletResponse.class), any(HttpSession.class));

        authController.getNewVerCode(request, response, session);

        verify(authService, times(1)).getVerificationCode(request, response, session);
    }

    @Test
    void testGetNewVerCode_Exception() {
        doThrow(new RuntimeException("test exception")).when(authService)
                .getVerificationCode(any(HttpServletRequest.class), any(HttpServletResponse.class), any(HttpSession.class));

        assertThrows(RuntimeException.class, () -> {
            authController.getNewVerCode(request, response, session);
        });
        verify(authService, times(1)).getVerificationCode(request, response, session);
    }

    @Test
    void testDoLogout_Success() {
        doNothing().when(authService).logout(any(HttpSession.class));

        String result = authController.doLogout(session);

        assertEquals("SUCCESS", result);
        verify(authService, times(1)).logout(session);
    }

    @Test
    void testDoChangePassword_Success() {
        OperationResult successResult = OperationResult.success("success");
        when(authService.changePassword(any(HttpServletRequest.class))).thenReturn(successResult);

        String result = authController.doChangePassword(request);

        assertEquals("success", result);
        verify(authService, times(1)).changePassword(request);
    }

    @Test
    void testDoChangePassword_Failure() {
        OperationResult failureResult = OperationResult.failure("oldpwderror");
        when(authService.changePassword(any(HttpServletRequest.class))).thenReturn(failureResult);

        String result = authController.doChangePassword(request);

        assertEquals("oldpwderror", result);
        verify(authService, times(1)).changePassword(request);
    }

    @Test
    void testDoChangePassword_NotLoggedIn() {
        OperationResult failureResult = OperationResult.failure("mustlogin");
        when(authService.changePassword(any(HttpServletRequest.class))).thenReturn(failureResult);

        String result = authController.doChangePassword(request);

        assertEquals("mustlogin", result);
        verify(authService, times(1)).changePassword(request);
    }

    @Test
    void testPong_LoggedIn() {
        when(authService.doPong(any(HttpServletRequest.class))).thenReturn("pong");

        String result = authController.pong(request);

        assertEquals("pong", result);
        verify(authService, times(1)).doPong(request);
    }

    @Test
    void testPong_NotLoggedIn() {
        when(authService.doPong(any(HttpServletRequest.class))).thenReturn("");

        String result = authController.pong(request);

        assertEquals("", result);
        verify(authService, times(1)).doPong(request);
    }

    @Test
    void testAskForAllowSignUpOrNot_True() {
        when(authService.isAllowSignUp()).thenReturn(true);

        String result = authController.askForAllowSignUpOrNot(request);

        assertEquals("true", result);
        verify(authService, times(1)).isAllowSignUp();
    }

    @Test
    void testAskForAllowSignUpOrNot_False() {
        when(authService.isAllowSignUp()).thenReturn(false);

        String result = authController.askForAllowSignUpOrNot(request);

        assertEquals("false", result);
        verify(authService, times(1)).isAllowSignUp();
    }

    @Test
    void testDoSigUp_Success() {
        OperationResult successResult = OperationResult.success("success");
        when(authService.signUp(any(HttpServletRequest.class))).thenReturn(successResult);

        String result = authController.doSigUp(request);

        assertEquals("success", result);
        verify(authService, times(1)).signUp(request);
    }

    @Test
    void testDoSigUp_Failure() {
        OperationResult failureResult = OperationResult.failure("accountexists");
        when(authService.signUp(any(HttpServletRequest.class))).thenReturn(failureResult);

        String result = authController.doSigUp(request);

        assertEquals("accountexists", result);
        verify(authService, times(1)).signUp(request);
    }

    @Test
    void testDoSigUp_InvalidAccount() {
        OperationResult failureResult = OperationResult.failure("invalidaccount");
        when(authService.signUp(any(HttpServletRequest.class))).thenReturn(failureResult);

        String result = authController.doSigUp(request);

        assertEquals("invalidaccount", result);
        verify(authService, times(1)).signUp(request);
    }

    @Test
    void testDoSigUp_Illegal() {
        OperationResult failureResult = OperationResult.failure("illegal");
        when(authService.signUp(any(HttpServletRequest.class))).thenReturn(failureResult);

        String result = authController.doSigUp(request);

        assertEquals("illegal", result);
        verify(authService, times(1)).signUp(request);
    }
}
