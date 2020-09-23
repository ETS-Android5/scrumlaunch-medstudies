/*
 * Copyright 2020 Google LLC
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package com.fdahpstudydesigner.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.fdahpstudydesigner.bean.AuditLogEventRequest;
import com.fdahpstudydesigner.config.HibernateTestConfig;
import com.fdahpstudydesigner.config.WebAppTestConfig;
import com.fdahpstudydesigner.service.AuditEventService;
import com.fdahpstudydesigner.util.FdahpStudyDesignerConstants;
import com.fdahpstudydesigner.util.SessionObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@PropertySource("classpath:application-mockit.properties")
@WebAppConfiguration("src/main/webapp")
@ContextConfiguration(classes = {WebAppTestConfig.class, HibernateTestConfig.class})
@TestExecutionListeners({
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class,
  TransactionalTestExecutionListener.class
})
public class BaseMockIT {

  @Resource private WebApplicationContext webAppContext;

  @Autowired private AuditEventService mockAuditService;

  protected final String CONTEXT_PATH = "/studybuilder";

  protected final String SESSION_USER_EMAIL = "mystudies_mockit@grr.la";

  protected MockMvc mockMvc;

  protected List<AuditLogEventRequest> auditRequests = new ArrayList<>();

  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
    initSecurityContext();
  }

  @After
  public void cleanUp() {
    auditRequests.clear();
  }

  @Test
  public void testMockMvcAndWebAppContext() {
    assertNotNull(webAppContext);
    assertNotNull(mockMvc);
  }

  protected void clearAuditRequests() {
    auditRequests.clear();
  }

  protected SessionObject getSessionObject() {
    SessionObject session = new SessionObject();
    session.setSessionId(UUID.randomUUID().toString());
    session.setEmail(SESSION_USER_EMAIL);
    return session;
  }

  protected HashMap<String, Object> getSessionAttributes() {
    HashMap<String, Object> sessionAttributes = new HashMap<String, Object>();
    sessionAttributes.put(FdahpStudyDesignerConstants.SESSION_OBJECT, getSessionObject());
    return sessionAttributes;
  }

  protected void initSecurityContext() {
    Authentication authentication = Mockito.mock(Authentication.class);
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  /**
   * @param assertOptionalFieldsForEvent is a {@link Map} collection that contains {@link eventCode}
   *     as key and {@link AuditLogEventRequest} with optional field values as value.
   * @param auditEvents audit event enums
   */
  protected void verifyAuditEventCall(
      Map<String, AuditLogEventRequest> assertOptionalFieldsForEvent,
      StudyBuilderAuditEvent... auditEvents) {

    verifyAuditEventCall(auditEvents);

    Map<String, AuditLogEventRequest> auditRequestByEventCode = new HashMap<>();
    for (AuditLogEventRequest auditRequest : auditRequests) {
      auditRequestByEventCode.put(auditRequest.getEventCode(), auditRequest);
    }

    for (Map.Entry<String, AuditLogEventRequest> entry : assertOptionalFieldsForEvent.entrySet()) {
      String eventCode = entry.getKey();
      AuditLogEventRequest expectedAuditRequest = entry.getValue();
      AuditLogEventRequest auditRequest = auditRequestByEventCode.get(eventCode);
      assertEquals(expectedAuditRequest.getUserId(), auditRequest.getUserId());
      assertEquals(expectedAuditRequest.getParticipantId(), auditRequest.getParticipantId());
      assertEquals(expectedAuditRequest.getStudyId(), auditRequest.getStudyId());
      assertEquals(expectedAuditRequest.getStudyVersion(), auditRequest.getStudyVersion());
    }
  }

  protected void verifyAuditEventCall(StudyBuilderAuditEvent... auditEvents) {
    ArgumentCaptor<AuditLogEventRequest> argument =
        ArgumentCaptor.forClass(AuditLogEventRequest.class);
    verify(mockAuditService, atLeastOnce()).postAuditLogEvent(argument.capture());

    Map<String, AuditLogEventRequest> auditRequestByEventCode = new HashMap<>();
    for (AuditLogEventRequest auditRequest : auditRequests) {
      auditRequestByEventCode.put(auditRequest.getEventCode(), auditRequest);
    }

    for (StudyBuilderAuditEvent auditEvent : auditEvents) {
      AuditLogEventRequest auditRequest = auditRequestByEventCode.get(auditEvent.getEventCode());

      assertEquals(auditEvent.getEventCode(), auditRequest.getEventCode());
      assertEquals(auditEvent.getDestination().getValue(), auditRequest.getDestination());
      assertEquals(auditEvent.getSource(), auditRequest.getSource());
      assertEquals(auditEvent.getResourceServer(), auditRequest.getResourceServer());

      assertFalse(
          StringUtils.contains(auditRequest.getDescription(), "{")
              && StringUtils.contains(auditRequest.getDescription(), "}"));
      assertNotNull(auditRequest.getCorrelationId());
      assertNotNull(auditRequest.getOccured());
      assertNotNull(auditRequest.getPlatformVersion());
      assertNotNull(auditRequest.getAppId());
      assertNotNull(auditRequest.getAppVersion());
      assertNotNull(auditRequest.getMobilePlatform());
    }
  }
}
