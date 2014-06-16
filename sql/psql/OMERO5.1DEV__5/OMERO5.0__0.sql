-- Copyright (C) 2012-4 Glencoe Software, Inc. All rights reserved.
-- Use is subject to license terms supplied in LICENSE.txt
--
-- This program is free software; you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation; either version 2 of the License, or
-- (at your option) any later version.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License along
-- with this program; if not, write to the Free Software Foundation, Inc.,
-- 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
--

---
--- OMERO5 development release upgrade from OMERO5.0__0 to OMERO5.1DEV__5.
---

BEGIN;

CREATE OR REPLACE FUNCTION omero_assert_db_version(version varchar, patch int) RETURNS void AS '
DECLARE
    rec RECORD;
BEGIN

    SELECT INTO rec *
           FROM dbpatch
          WHERE id = ( SELECT id FROM dbpatch ORDER BY id DESC LIMIT 1 )
            AND currentversion = version
            AND currentpatch = patch;

    IF NOT FOUND THEN
        RAISE EXCEPTION ''ASSERTION ERROR: Wrong database version'';
    END IF;

END;' LANGUAGE plpgsql;

SELECT omero_assert_db_version('OMERO5.0', 0);
DROP FUNCTION omero_assert_db_version(varchar, int);


INSERT INTO dbpatch (currentVersion, currentPatch,   previousVersion,     previousPatch)
             VALUES ('OMERO5.1DEV',     5,              'OMERO5.0',       0);

--
-- Actual upgrade
--

ALTER TABLE session ADD COLUMN userIP varchar(15);
ALTER TABLE logicalchannel ALTER COLUMN emissionWave TYPE FLOAT8;
ALTER TABLE logicalchannel ALTER COLUMN excitationWave TYPE FLOAT8;
ALTER TABLE laser ALTER COLUMN wavelength TYPE FLOAT8;
ALTER TABLE lightsettings ALTER COLUMN wavelength TYPE FLOAT8;

-- #11877 move import logs to upload jobs so they are no longer file annotations
-- may have been missed in 5.0 by users starting from 5.0RC1
CREATE FUNCTION upgrade_import_logs() RETURNS void AS $$

    DECLARE
        import      RECORD;
        time_now    TIMESTAMP WITHOUT TIME ZONE;
        event_type  BIGINT;
        event_id    BIGINT;
        new_link_id BIGINT;

    BEGIN
        SELECT id INTO STRICT event_type FROM eventtype WHERE value = 'Internal';

        FOR import IN
            SELECT fal.id AS old_link_id, a.id AS annotation_id, u.job_id AS job_id, a.file AS log_id
              FROM filesetannotationlink fal, annotation a, filesetjoblink fjl, uploadjob u
             WHERE fal.parent = fjl.parent AND fal.child = a.id AND fjl.child = u.job_id
               AND a.discriminator = '/type/OriginalFile/' AND a.ns = 'openmicroscopy.org/omero/import/logFile' LOOP

            SELECT clock_timestamp() INTO time_now;
            SELECT ome_nextval('seq_event') INTO event_id;
            SELECT ome_nextval('seq_joboriginalfilelink') INTO new_link_id;

            INSERT INTO event (id, permissions, "time", experimenter, experimentergroup, session, type)
                SELECT event_id, a.permissions, time_now, a.owner_id, a.group_id, 0, event_type
                  FROM annotation a WHERE a.id = import.annotation_id;

            INSERT INTO eventlog (id, action, permissions, entityid, entitytype, event)
                SELECT ome_nextval('seq_eventlog'), 'INSERT', e.permissions, new_link_id, 'ome.model.jobs.JobOriginalFileLink', event_id
                  FROM event e WHERE e.id = event_id;

            INSERT INTO joboriginalfilelink (id, permissions, creation_id, update_id, owner_id, group_id, parent, child)
                SELECT new_link_id, old_link.permissions, old_link.creation_id, old_link.update_id, old_link.owner_id, old_link.group_id, import.job_id, import.log_id
                  FROM filesetannotationlink old_link WHERE old_link.id = import.old_link_id;

            UPDATE originalfile SET mimetype = 'application/omero-log-file' WHERE id = import.log_id;

            DELETE FROM annotationannotationlink WHERE parent = import.annotation_id OR child = import.annotation_id;
            DELETE FROM channelannotationlink WHERE child = import.annotation_id;
            DELETE FROM datasetannotationlink WHERE child = import.annotation_id;
            DELETE FROM experimenterannotationlink WHERE child = import.annotation_id;
            DELETE FROM experimentergroupannotationlink WHERE child = import.annotation_id;
            DELETE FROM filesetannotationlink WHERE child = import.annotation_id;
            DELETE FROM imageannotationlink WHERE child = import.annotation_id;
            DELETE FROM namespaceannotationlink WHERE child = import.annotation_id;
            DELETE FROM nodeannotationlink WHERE child = import.annotation_id;
            DELETE FROM originalfileannotationlink WHERE child = import.annotation_id;
            DELETE FROM pixelsannotationlink WHERE child = import.annotation_id;
            DELETE FROM planeinfoannotationlink WHERE child = import.annotation_id;
            DELETE FROM plateacquisitionannotationlink WHERE child = import.annotation_id;
            DELETE FROM plateannotationlink WHERE child = import.annotation_id;
            DELETE FROM projectannotationlink WHERE child = import.annotation_id;
            DELETE FROM reagentannotationlink WHERE child = import.annotation_id;
            DELETE FROM roiannotationlink WHERE child = import.annotation_id;
            DELETE FROM screenannotationlink WHERE child = import.annotation_id;
            DELETE FROM sessionannotationlink WHERE child = import.annotation_id;
            DELETE FROM wellannotationlink WHERE child = import.annotation_id;
            DELETE FROM wellsampleannotationlink WHERE child = import.annotation_id;
            DELETE FROM annotation WHERE id = import.annotation_id;
        END LOOP;
    END;
$$ LANGUAGE plpgsql;

SELECT upgrade_import_logs();

DROP FUNCTION upgrade_import_logs();

-- #11664 fix brittleness of _fs_deletelog()
CREATE OR REPLACE FUNCTION _fs_log_delete() RETURNS TRIGGER AS $_fs_log_delete$
    BEGIN
        IF OLD.repo IS NOT NULL THEN
            INSERT INTO _fs_deletelog (event_id, file_id, owner_id, group_id, "path", "name", repo, params)
                SELECT _current_or_new_event(), OLD.id, OLD.owner_id, OLD.group_id, OLD."path", OLD."name", OLD.repo, OLD.params;
        END IF;
        RETURN OLD;
    END;
$_fs_log_delete$ LANGUAGE plpgsql;

-- #11663 SQL DOMAIN types
CREATE DOMAIN nonnegative_int AS INTEGER CHECK (VALUE >= 0);
CREATE DOMAIN positive_int AS INTEGER CHECK (VALUE > 0);
CREATE DOMAIN positive_float AS DOUBLE PRECISION CHECK (VALUE > 0);
CREATE DOMAIN percent_fraction AS DOUBLE PRECISION CHECK (VALUE >= 0 AND VALUE <= 1);

ALTER TABLE detectorsettings ALTER COLUMN integration TYPE positive_int;
ALTER TABLE detectorsettings DROP CONSTRAINT detectorsettings_integration_check;

ALTER TABLE imagingenvironment ALTER COLUMN co2percent TYPE percent_fraction;
ALTER TABLE imagingenvironment ALTER COLUMN humidity TYPE percent_fraction;
ALTER TABLE imagingenvironment DROP CONSTRAINT imagingenvironment_check;

ALTER TABLE laser ALTER COLUMN frequencyMultiplication TYPE positive_int;
ALTER TABLE laser ALTER COLUMN wavelength TYPE positive_float;
ALTER TABLE laser DROP CONSTRAINT laser_check;

ALTER TABLE lightsettings ALTER COLUMN attenuation TYPE percent_fraction;
ALTER TABLE lightsettings ALTER COLUMN wavelength TYPE positive_float;
ALTER TABLE lightsettings DROP CONSTRAINT lightsettings_check;

ALTER TABLE logicalchannel ALTER COLUMN emissionWave TYPE positive_float;
ALTER TABLE logicalchannel ALTER COLUMN excitationWave TYPE positive_float;
ALTER TABLE logicalchannel ALTER COLUMN samplesPerPixel TYPE positive_int;
ALTER TABLE logicalchannel DROP CONSTRAINT logicalchannel_check;

ALTER TABLE otf ALTER COLUMN sizeX TYPE positive_int;
ALTER TABLE otf ALTER COLUMN sizeY TYPE positive_int;
ALTER TABLE otf DROP CONSTRAINT otf_check;

UPDATE pixels SET physicalSizeX = NULL WHERE physicalSizeX <= 0;
UPDATE pixels SET physicalSizeY = NULL WHERE physicalSizeY <= 0;
UPDATE pixels SET physicalSizeZ = NULL WHERE physicalSizeZ <= 0;

ALTER TABLE pixels ALTER COLUMN physicalSizeX TYPE positive_float;
ALTER TABLE pixels ALTER COLUMN physicalSizeY TYPE positive_float;
ALTER TABLE pixels ALTER COLUMN physicalSizeZ TYPE positive_float;
ALTER TABLE pixels ALTER COLUMN significantBits TYPE positive_int;
ALTER TABLE pixels ALTER COLUMN sizeC TYPE positive_int;
ALTER TABLE pixels ALTER COLUMN sizeT TYPE positive_int;
ALTER TABLE pixels ALTER COLUMN sizeX TYPE positive_int;
ALTER TABLE pixels ALTER COLUMN sizeY TYPE positive_int;
ALTER TABLE pixels ALTER COLUMN sizeZ TYPE positive_int;
ALTER TABLE pixels DROP CONSTRAINT pixels_check;

ALTER TABLE planeinfo ALTER COLUMN theC TYPE nonnegative_int;
ALTER TABLE planeinfo ALTER COLUMN theT TYPE nonnegative_int;
ALTER TABLE planeinfo ALTER COLUMN theZ TYPE nonnegative_int;
ALTER TABLE planeinfo DROP CONSTRAINT planeinfo_check;

ALTER TABLE transmittancerange ALTER COLUMN cutIn TYPE positive_int;
ALTER TABLE transmittancerange ALTER COLUMN cutInTolerance TYPE nonnegative_int;
ALTER TABLE transmittancerange ALTER COLUMN cutOut TYPE positive_int;
ALTER TABLE transmittancerange ALTER COLUMN cutOutTolerance TYPE nonnegative_int;
ALTER TABLE transmittancerange ALTER COLUMN transmittance TYPE percent_fraction;
ALTER TABLE transmittancerange DROP CONSTRAINT transmittancerange_check;

-- #12126

UPDATE pixelstype SET bitsize = 16 WHERE value = 'uint16';

-- # map annotation

CREATE TABLE annotation_mapValue (
    annotation_id INT8 NOT NULL,
    mapValue VARCHAR(255) NOT NULL,
    mapValue_key VARCHAR(255),
    PRIMARY KEY (annotation_id, mapValue_key),
    CONSTRAINT FKF96E60858062A40 
        FOREIGN KEY (annotation_id) 
        REFERENCES annotation
);

CREATE TABLE experimentergroup_config (
    experimentergroup_id INT8 NOT NULL,
    config VARCHAR(255) NOT NULL,
    config_key VARCHAR(255),
    PRIMARY KEY (experimentergroup_id, config_key),
    CONSTRAINT FKDC631B6CF5F0705D 
        FOREIGN KEY (experimentergroup_id) 
        REFERENCES experimentergroup
);

CREATE TABLE genericexcitationsource (
    lightsource_id INT8 PRIMARY KEY,
    CONSTRAINT FKgenericexcitationsource_lightsource_id_lightsource 
        FOREIGN KEY (lightsource_id) 
        REFERENCES lightsource
);

CREATE TABLE genericexcitationsource_map (
    genericexcitationsource_id INT8 NOT NULL,
    "map" VARCHAR(255) NOT NULL,
    map_key VARCHAR(255),
    PRIMARY KEY (genericexcitationsource_id, map_key),
    CONSTRAINT FK7B28ABA9C1805FCD 
        FOREIGN KEY (genericexcitationsource_id) 
        REFERENCES genericexcitationsource
);

CREATE TABLE imagingenvironment_map (
    imagingenvironment_id INT8 NOT NULL,
    "map" VARCHAR(255) NOT NULL,
    map_key VARCHAR(255),
    PRIMARY KEY (imagingenvironment_id, map_key),
    CONSTRAINT FK7C8DCED8CDF68A87 
        FOREIGN KEY (imagingenvironment_id) 
        REFERENCES imagingenvironment
);

-- #12193: replace FilesetVersionInfo with map property on Fileset

CREATE TABLE metadataimportjob_versioninfo (
    metadataimportjob_id INT8 NOT NULL,
    versioninfo VARCHAR(255) NOT NULL,
    versioninfo_key VARCHAR(255),
    PRIMARY KEY (metadataimportjob_id, versioninfo_key),
    CONSTRAINT FK947FE61023506BCE 
        FOREIGN KEY (metadataimportjob_id) 
        REFERENCES metadataimportjob
);

CREATE TABLE uploadjob_versioninfo (
    uploadjob_id INT8 NOT NULL,
    versioninfo VARCHAR(255) NOT NULL,
    versioninfo_key VARCHAR(255),
    PRIMARY KEY (uploadjob_id, versioninfo_key),
    CONSTRAINT FK3B5720031800070E 
        FOREIGN KEY (uploadjob_id) 
        REFERENCES uploadjob
);

INSERT INTO metadataimportjob_versioninfo (metadataimportjob_id, versioninfo_key, versioninfo)
    SELECT metadataimportjob.job_id, 'bioformats.reader', filesetversioninfo.bioformatsreader
    FROM filesetversioninfo, metadataimportjob
    WHERE filesetversioninfo.id = metadataimportjob.versioninfo;

INSERT INTO metadataimportjob_versioninfo (metadataimportjob_id, versioninfo_key, versioninfo)
    SELECT metadataimportjob.job_id, 'bioformats.version', filesetversioninfo.bioformatsversion
    FROM filesetversioninfo, metadataimportjob
    WHERE filesetversioninfo.id = metadataimportjob.versioninfo;

INSERT INTO metadataimportjob_versioninfo (metadataimportjob_id, versioninfo_key, versioninfo)
    SELECT metadataimportjob.job_id, 'locale', filesetversioninfo.locale
    FROM filesetversioninfo, metadataimportjob
    WHERE filesetversioninfo.id = metadataimportjob.versioninfo;

INSERT INTO metadataimportjob_versioninfo (metadataimportjob_id, versioninfo_key, versioninfo)
    SELECT metadataimportjob.job_id, 'omero.version', filesetversioninfo.omeroversion
    FROM filesetversioninfo, metadataimportjob
    WHERE filesetversioninfo.id = metadataimportjob.versioninfo;

INSERT INTO metadataimportjob_versioninfo (metadataimportjob_id, versioninfo_key, versioninfo)
    SELECT metadataimportjob.job_id, 'os.name', filesetversioninfo.osname
    FROM filesetversioninfo, metadataimportjob
    WHERE filesetversioninfo.id = metadataimportjob.versioninfo;

INSERT INTO metadataimportjob_versioninfo (metadataimportjob_id, versioninfo_key, versioninfo)
    SELECT metadataimportjob.job_id, 'os.version', filesetversioninfo.osversion
    FROM filesetversioninfo, metadataimportjob
    WHERE filesetversioninfo.id = metadataimportjob.versioninfo;

INSERT INTO metadataimportjob_versioninfo (metadataimportjob_id, versioninfo_key, versioninfo)
    SELECT metadataimportjob.job_id, 'os.architecture', filesetversioninfo.osarchitecture
    FROM filesetversioninfo, metadataimportjob
    WHERE filesetversioninfo.id = metadataimportjob.versioninfo;

INSERT INTO uploadjob_versioninfo (uploadjob_id, versioninfo_key, versioninfo)
    SELECT uploadjob.job_id, 'bioformats.reader', filesetversioninfo.bioformatsreader
    FROM filesetversioninfo, uploadjob
    WHERE filesetversioninfo.id = uploadjob.versioninfo;

INSERT INTO uploadjob_versioninfo (uploadjob_id, versioninfo_key, versioninfo)
    SELECT uploadjob.job_id, 'bioformats.version', filesetversioninfo.bioformatsversion
    FROM filesetversioninfo, uploadjob
    WHERE filesetversioninfo.id = uploadjob.versioninfo;

INSERT INTO uploadjob_versioninfo (uploadjob_id, versioninfo_key, versioninfo)
    SELECT uploadjob.job_id, 'locale', filesetversioninfo.locale
    FROM filesetversioninfo, uploadjob
    WHERE filesetversioninfo.id = uploadjob.versioninfo;

INSERT INTO uploadjob_versioninfo (uploadjob_id, versioninfo_key, versioninfo)
    SELECT uploadjob.job_id, 'omero.version', filesetversioninfo.omeroversion
    FROM filesetversioninfo, uploadjob
    WHERE filesetversioninfo.id = uploadjob.versioninfo;

INSERT INTO uploadjob_versioninfo (uploadjob_id, versioninfo_key, versioninfo)
    SELECT uploadjob.job_id, 'os.name', filesetversioninfo.osname
    FROM filesetversioninfo, uploadjob
    WHERE filesetversioninfo.id = uploadjob.versioninfo;

INSERT INTO uploadjob_versioninfo (uploadjob_id, versioninfo_key, versioninfo)
    SELECT uploadjob.job_id, 'os.version', filesetversioninfo.osversion
    FROM filesetversioninfo, uploadjob
    WHERE filesetversioninfo.id = uploadjob.versioninfo;

INSERT INTO uploadjob_versioninfo (uploadjob_id, versioninfo_key, versioninfo)
    SELECT uploadjob.job_id, 'os.architecture', filesetversioninfo.osarchitecture
    FROM filesetversioninfo, uploadjob
    WHERE filesetversioninfo.id = uploadjob.versioninfo;

ALTER TABLE metadataimportjob DROP COLUMN versioninfo;
ALTER TABLE uploadjob DROP COLUMN versioninfo;

DROP SEQUENCE seq_filesetversioninfo;
DROP TABLE filesetversioninfo;

-- it is not worth keeping these uninformative rows

DELETE FROM metadataimportjob_versioninfo WHERE versioninfo = 'Unknown';
DELETE FROM uploadjob_versioninfo WHERE versioninfo = 'Unknown';

-- #12242: Bug: broken upgrade of nightshade
-- #11479: https://github.com/openmicroscopy/openmicroscopy/pull/2369#issuecomment-41701620
-- So, remove annotations with bad discriminators or inter-group links.

-- return if the group IDs include multiple non-user groups
CREATE FUNCTION is_too_many_group_ids(VARIADIC group_ids BIGINT[]) RETURNS BOOLEAN AS $$

    DECLARE
        user_group  BIGINT;
        other_group BIGINT;
        curr_group  BIGINT;
        index       BIGINT;

    BEGIN
        SELECT id INTO user_group FROM experimentergroup WHERE name = 'user';

        FOR index IN 1 .. array_upper(group_ids, 1) LOOP
            curr_group := group_ids[index];
            CONTINUE WHEN user_group = curr_group;
            IF other_group IS NULL THEN
                other_group := curr_group;
            ELSIF other_group != curr_group THEN
                RETURN TRUE;
            END IF;
        END LOOP;

        RETURN FALSE;
    END;

$$ LANGUAGE plpgsql;

DELETE FROM annotationannotationlink link
      USING annotation parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (parent.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             child.discriminator  IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM channelannotationlink link
      USING channel parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM datasetannotationlink link
      USING dataset parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM experimenterannotationlink link
      USING annotation child
      WHERE link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(link.group_id, child.group_id));

DELETE FROM experimentergroupannotationlink link
      USING annotation child
      WHERE link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(link.group_id, child.group_id));

DELETE FROM filesetannotationlink link
      USING fileset parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM imageannotationlink link
      USING image parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM namespaceannotationlink link
      USING namespace parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM nodeannotationlink link
      USING annotation child
      WHERE link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(link.group_id, child.group_id));

DELETE FROM originalfileannotationlink link
      USING originalfile parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM pixelsannotationlink link
      USING pixels parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM planeinfoannotationlink link
      USING planeinfo parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM plateacquisitionannotationlink link
      USING plateacquisition parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM plateannotationlink link
      USING plate parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM projectannotationlink link
      USING project parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM reagentannotationlink link
      USING reagent parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM roiannotationlink link
      USING roi parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM screenannotationlink link
      USING screen parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM sessionannotationlink link
      USING annotation child
      WHERE link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(link.group_id, child.group_id));

DELETE FROM wellannotationlink link
      USING well parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DELETE FROM wellsampleannotationlink link
      USING wellsample parent, annotation child
      WHERE link.parent = parent.id AND link.child = child.id
        AND (child.discriminator IN ('/basic/text/uri/', '/basic/text/url/') OR
             is_too_many_group_ids(parent.group_id, link.group_id, child.group_id));

DROP FUNCTION is_too_many_group_ids(VARIADIC group_ids BIGINT[]);

DELETE FROM annotation
      WHERE discriminator IN ('/basic/text/uri/', '/basic/text/url/');

DELETE FROM configuration
      WHERE name = 'DB check DBBadAnnotationCheck';

--
-- FINISHED
--

UPDATE dbpatch SET message = 'Database updated.', finished = clock_timestamp()
    WHERE currentVersion  = 'OMERO5.1DEV' AND
          currentPatch    = 5             AND
          previousVersion = 'OMERO5.0'    AND
          previousPatch   = 0;

SELECT CHR(10)||CHR(10)||CHR(10)||'YOU HAVE SUCCESSFULLY UPGRADED YOUR DATABASE TO VERSION OMERO5.1DEV__5'||CHR(10)||CHR(10)||CHR(10) AS Status;

COMMIT;
