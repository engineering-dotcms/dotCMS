--Dettaglio
update contentlet set date1=date4 where date4 is not null and structure_inode='b311ef56-92f2-403a-8373-e7d2b8480693';
--Dettaglio Contenitore
update contentlet set date4=date3 where date3 is not null and structure_inode='48a2ace7-af5f-409b-9885-bf2d80bbe25a';
--Cambio
update contentlet set date7=date4 where date4 is not null and structure_inode='d39eec79-eb5e-4602-a442-b93f7bcf1c65';
--Foto
update contentlet set date6=date3 where date3 is not null and structure_inode='29fc83ae-3b72-46ef-b62c-110f1786f52c';
--Video
update contentlet set date7=date4 where date4 is not null and structure_inode='e65543eb-6b81-42e0-a59b-1bb9fd7bfce4';
--Video Content
update contentlet set date8=date4 where date4 is not null and structure_inode='d93596c3-578d-4fa0-bdb6-3a40be73c13a';
--allegato dettaglio manca time Modified posso usare data emanazione
update contentlet set date6=date1 where date1 is not null and structure_inode='963f29ca-d36b-439f-a727-af28716fbc1e';
--link incoerenti
update contentlet set text14=text3 where structure_inode='02ed7608-1671-4268-ba98-6a56a40fadfa' and text3!=text14

--allegato dettaglio manca time Modified posso usare data emanazione
--update contentlet set date6=date1 where date1 is not null and structure_inode='963f29ca-d36b-439f-a727-af28716fbc1e';
--uso time modified del link
/*MERGE INTO contentlet
USING(SELECT  callegato.inode AS allegatoInode, min(clink.date3) as timeModified
FROM    contentlet callegato,contentlet clink,contentlet_version_info vallegato,contentlet_version_info vlink
WHERE   callegato.identifier=clink.text14 and callegato.language_id=clink.language_id and 
callegato.structure_inode='963f29ca-d36b-439f-a727-af28716fbc1e' and clink.structure_inode='02ed7608-1671-4268-ba98-6a56a40fadfa' 
and callegato.identifier=vallegato.identifier and callegato.inode=vallegato.live_inode and callegato.language_id=vallegato.lang
and clink.identifier=vlink.identifier and clink.inode=vlink.live_inode and clink.language_id=vlink.lang
group by callegato.inode)
ON (inode=allegatoInode)
WHEN MATCHED THEN
UPDATE
SET date6=timeModified;*/

--allert dai link
MERGE INTO contentlet
USING(SELECT  callegato.inode AS allegatoInode, max(clink.text9) as linkAlert 
FROM    contentlet callegato,contentlet clink,contentlet_version_info vallegato,contentlet_version_info vlink 
WHERE   callegato.identifier=clink.text14 and callegato.language_id=clink.language_id and 
callegato.structure_inode='963f29ca-d36b-439f-a727-af28716fbc1e' and clink.structure_inode='02ed7608-1671-4268-ba98-6a56a40fadfa' 
and callegato.identifier=vallegato.identifier and callegato.inode=vallegato.live_inode and callegato.language_id=vallegato.lang 
and clink.identifier=vlink.identifier and clink.inode=vlink.live_inode and clink.language_id=vlink.lang and clink.text9 is not null
group by callegato.inode)
ON (inode=allegatoInode)
WHEN MATCHED THEN
UPDATE
SET text8=linkAlert;
