/*
 * Copyright 2020 Google LLC
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package com.google.cloud.healthcare.fdamystudies.dao;

import com.google.cloud.healthcare.fdamystudies.bean.StudyMetadataBean;
import com.google.cloud.healthcare.fdamystudies.beans.ErrorBean;
import com.google.cloud.healthcare.fdamystudies.common.Permission;
import com.google.cloud.healthcare.fdamystudies.model.AppEntity;
import com.google.cloud.healthcare.fdamystudies.model.AppPermissionEntity;
import com.google.cloud.healthcare.fdamystudies.model.LocationEntity;
import com.google.cloud.healthcare.fdamystudies.model.SiteEntity;
import com.google.cloud.healthcare.fdamystudies.model.StudyEntity;
import com.google.cloud.healthcare.fdamystudies.model.StudyPermissionEntity;
import com.google.cloud.healthcare.fdamystudies.model.UserRegAdminEntity;
import com.google.cloud.healthcare.fdamystudies.util.AppConstants;
import com.google.cloud.healthcare.fdamystudies.util.ErrorCode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class StudiesDaoImpl implements StudiesDao {

  private static final Logger logger = LoggerFactory.getLogger(StudiesDaoImpl.class);

  @Autowired private SessionFactory sessionFactory;

  @Override
  public ErrorBean saveStudyMetadata(StudyMetadataBean studyMetadataBean) {
    logger.info("StudiesDaoImpl - saveStudyMetadata() : Starts");
    CriteriaBuilder builder = null;
    CriteriaQuery<StudyEntity> studyCriteria = null;
    Root<StudyEntity> studyRoot = null;
    Predicate[] studyPredicate = new Predicate[1];
    StudyEntity studyInfo = null;

    CriteriaQuery<AppEntity> appCriteria = null;
    Root<AppEntity> appRoot = null;
    Predicate[] appPredicate = new Predicate[1];

    AppEntity appInfo = null;

    List<UserRegAdminEntity> superAdminUser = new ArrayList<>();

    ErrorBean errorBean = null;
    Session session = this.sessionFactory.getCurrentSession();
    builder = session.getCriteriaBuilder();
    studyCriteria = builder.createQuery(StudyEntity.class);
    studyRoot = studyCriteria.from(StudyEntity.class);
    studyPredicate[0] = builder.equal(studyRoot.get("customId"), studyMetadataBean.getStudyId());
    studyCriteria.select(studyRoot).where(studyPredicate);
    studyInfo = session.createQuery(studyCriteria).uniqueResult();

    appCriteria = builder.createQuery(AppEntity.class);
    appRoot = appCriteria.from(AppEntity.class);
    appPredicate[0] = builder.equal(appRoot.get("appId"), studyMetadataBean.getAppId());
    appCriteria.select(appRoot).where(appPredicate);
    appInfo = session.createQuery(appCriteria).uniqueResult();

    CriteriaQuery<UserRegAdminEntity> urAdminUserCriteria =
        builder.createQuery(UserRegAdminEntity.class);
    Root<UserRegAdminEntity> urAdminUserRoot = urAdminUserCriteria.from(UserRegAdminEntity.class);
    Predicate[] urAdminUserPredicate = new Predicate[1];
    urAdminUserPredicate[0] = builder.equal(urAdminUserRoot.get("superAdmin"), true);
    urAdminUserCriteria.select(urAdminUserRoot).where(urAdminUserPredicate);
    superAdminUser = session.createQuery(urAdminUserCriteria).getResultList();

    if (studyInfo != null) {

      appInfo.setAppId(studyMetadataBean.getAppId());
      appInfo.setAppName(studyMetadataBean.getAppName());
      appInfo.setAppDescription(studyMetadataBean.getAppDescription());
      appInfo.setModifiedBy(String.valueOf(0));
      appInfo.setModified(Timestamp.from(Instant.now()));

      studyInfo.setCustomId(studyMetadataBean.getStudyId());
      studyInfo.setName(studyMetadataBean.getStudyTitle());
      studyInfo.setVersion(Float.valueOf(studyMetadataBean.getStudyVersion()));
      studyInfo.setType(studyMetadataBean.getStudyType());
      studyInfo.setStatus(studyMetadataBean.getStudyStatus());
      studyInfo.setCategory(studyMetadataBean.getStudyCategory());
      studyInfo.setTagline(studyMetadataBean.getStudyTagline());
      studyInfo.setSponsor(studyMetadataBean.getStudySponsor());
      studyInfo.setEnrolling(studyMetadataBean.getStudyEnrolling());
      studyInfo.setApp(appInfo);
      studyInfo.setModifiedBy(String.valueOf(0));
      studyInfo.setModified(Timestamp.from(Instant.now()));
      session.update(studyInfo);
      if (studyInfo.getStatus().equalsIgnoreCase("Deactivated")) {
        decommisionSiteFromStudy(session, studyInfo);
      }
    } else {

      if (appInfo == null) {
        appInfo = new AppEntity();
        appInfo.setAppId(studyMetadataBean.getAppId());
        appInfo.setAppName(studyMetadataBean.getAppName());
        appInfo.setAppDescription(studyMetadataBean.getAppDescription());
        appInfo.setCreatedBy(String.valueOf(0));
        appInfo.setCreated(Timestamp.from(Instant.now()));

        session.save(appInfo);

        for (UserRegAdminEntity user : superAdminUser) {
          AppPermissionEntity appPermission = new AppPermissionEntity();
          appPermission.setApp(appInfo);
          appPermission.setUrAdminUser(user);
          appPermission.setEdit(Permission.EDIT);
          appPermission.setCreated(Timestamp.from(Instant.now()));
          appPermission.setCreatedBy(user.getId());
          session.save(appPermission);
        }
      }

      studyInfo = new StudyEntity();
      studyInfo.setCustomId(studyMetadataBean.getStudyId());
      studyInfo.setName(studyMetadataBean.getStudyTitle());
      studyInfo.setVersion(Float.valueOf(studyMetadataBean.getStudyVersion()));
      studyInfo.setType(studyMetadataBean.getStudyType());
      studyInfo.setStatus(studyMetadataBean.getStudyStatus());
      studyInfo.setCategory(studyMetadataBean.getStudyCategory());
      studyInfo.setTagline(studyMetadataBean.getStudyTagline());
      studyInfo.setSponsor(studyMetadataBean.getStudySponsor());
      studyInfo.setEnrolling(studyMetadataBean.getStudyEnrolling());
      studyInfo.setApp(appInfo);
      studyInfo.setCreatedBy(String.valueOf(0));
      studyInfo.setCreated(Timestamp.from(Instant.now()));
      String generatedStudyid = (String) session.save(studyInfo);

      for (UserRegAdminEntity user : superAdminUser) {
        StudyPermissionEntity studyPermission = new StudyPermissionEntity();
        studyPermission.setApp(appInfo);
        studyPermission.setStudy(studyInfo);
        studyPermission.setUrAdminUser(user);
        studyPermission.setEdit(Permission.EDIT);
        studyPermission.setCreated(Timestamp.from(Instant.now()));
        studyPermission.setCreatedBy(user.getId());
        session.save(studyPermission);
      }

      if (!StringUtils.isBlank(studyMetadataBean.getStudyType())
          && studyMetadataBean.getStudyType().equals(AppConstants.OPEN_STUDY)) {
        LocationEntity defaultLocation =
            (LocationEntity)
                session.createQuery("from LocationBo where isdefault='Y'").getSingleResult();
        if (defaultLocation != null) {
          StudyEntity studyInfoCreated = session.get(StudyEntity.class, generatedStudyid);
          SiteEntity site = new SiteEntity();
          site.setStudy(studyInfoCreated);
          site.setLocation(defaultLocation);
          site.setCreatedBy(String.valueOf(0));
          site.setStatus(1);
          site.setTargetEnrollment(0);
          session.save(site);
        }
      }
    }

    errorBean = new ErrorBean(ErrorCode.EC_200.code(), ErrorCode.EC_200.errorMessage());
    logger.info("StudiesDaoImpl - saveStudyMetadata() : ends");
    return errorBean;
  }

  private void decommisionSiteFromStudy(Session session, StudyEntity study) {
    logger.info("StudiesDaoImpl - decommisionSiteFromStudy() : Starts");
    CriteriaBuilder builder = null;
    CriteriaQuery<SiteEntity> siteCriteria = null;
    Root<SiteEntity> siteRoot = null;
    Predicate[] sitePredicate = new Predicate[1];
    List<SiteEntity> siteList = null;
    builder = session.getCriteriaBuilder();
    siteCriteria = builder.createQuery(SiteEntity.class);
    siteRoot = siteCriteria.from(SiteEntity.class);
    sitePredicate[0] = builder.equal(siteRoot.get("study"), study);
    siteCriteria.select(siteRoot).where(sitePredicate);
    siteList = session.createQuery(siteCriteria).getResultList();
    if (!siteList.isEmpty()) {
      for (SiteEntity site : siteList) {
        site.setStatus(0);
        site.setModifiedBy(String.valueOf(0));
        site.setModified(Timestamp.from(Instant.now()));
        session.update(site);
      }
    }
    logger.info("StudiesDaoImpl - decommisionSiteFromStudy() : ends");
  }
}
