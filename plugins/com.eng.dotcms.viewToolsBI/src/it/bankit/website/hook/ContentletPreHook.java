package it.bankit.website.hook;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHookAbstractImp;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class ContentletPreHook extends ContentletAPIPreHookAbstractImp {

	private static final Logger LOG = Logger.getLogger(ContentletPreHook.class);

	private static final String dataPubblicazioneFieldName = "dataPubblicazione";
	private static final String dataCreazioneFieldName = "timeCreated";
	private static final String changedDateFieldName = "changeddate";

	private static final String[] dateToCheck = {"sysPublishDate", "sysExpireDate", "dataEmanazione"};

	private static final String allegatoDettaglio = "AllegatoDettaglio";
	private static final String link = "Link";

	@Override
	public boolean checkin(Contentlet contentlet, List<Permission> permissions, User user, boolean respectFrontendRoles) {
		return super.checkin(contentlet, permissions, user, respectFrontendRoles);
	}

	@Override
	public boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, List<Permission> permissions, User user, boolean respectFrontendRoles) {
		if (needDataCreazione(contentlet)) {
			setCreationDate(contentlet);
		}
		checkAllDateFieldsValue(contentlet);
		return super.checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
	}

	@Override
	public boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats, User user, boolean respectFrontendRoles) {
		if (needDataCreazione(contentlet)) {
			setCreationDate(contentlet);
		}
		checkAllDateFieldsValue(contentlet);
		return super.checkin(contentlet, contentRelationships, cats, user, respectFrontendRoles);
	}

	@Override
	public boolean archive(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return super.archive(contentlet, user, respectFrontendRoles);
	}

	@Override
	public boolean archive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		// TODO Auto-generated method stub
		return super.archive(contentlets, user, respectFrontendRoles);
	}

	@Override
	public boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions) {
		if (!getLinkRelated(contentlet)) {
			return super.delete(contentlet, user, respectFrontendRoles);
		} else {
			return false;
		}
	}

	@Override
	public boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		if (!getLinkRelated(contentlet)) {
			return super.delete(contentlet, user, respectFrontendRoles);
		} else {
			return false;
		}
	}

	@Override
	public boolean delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles, boolean allVersions) {
		List<Contentlet> contentletsnew = new ArrayList<Contentlet>();
		for (Contentlet contentlet2 : contentlets) {
			if (isStructureType(contentlet2, allegatoDettaglio)) {
				if (!getLinkRelated(contentlet2)) {
					// contentlets.remove(contentlet2);
					// System.out.println("Non posso rimuovere il contenuto - "
					// + contentlet2.getTitle() + " - perchè relazionato!");
					contentletsnew.add(contentlet2);
				}
			}
			contentletsnew.add(contentlet2);
		}
		return super.delete(contentletsnew, user, respectFrontendRoles, allVersions);
	}

	@Override
	public boolean delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		List<Contentlet> contentletsnew = new ArrayList<Contentlet>();

		for (Contentlet contentlet2 : contentlets) {
			if (isStructureType(contentlet2, allegatoDettaglio)) {
				if (!getLinkRelated(contentlet2)) {
					// contentlets.remove(contentlet2);
					// System.out.println("Non posso rimuovere il contenuto - "
					// + contentlet2.getTitle() + " - perchè relazionato!");
					contentletsnew.add(contentlet2);
				}
			}
			contentletsnew.add(contentlet2);
		}

		return super.delete(contentletsnew, user, respectFrontendRoles);
	}

	@Override
	public boolean checkin(Contentlet contentlet, ContentletRelationships relationshipsData, List<Category> cats, List<Permission> selectedPermissions, User user, boolean respectFrontendRoles) {
		if (needDataCreazione(contentlet)) {
			setCreationDate(contentlet);
		}
		checkAllDateFieldsValue(contentlet);
		return super.checkin(contentlet, relationshipsData, cats, selectedPermissions, user, respectFrontendRoles);
	}

	@Override
	public boolean checkin(Contentlet contentlet, List<Category> cats, List<Permission> permissions, User user, boolean respectFrontendRoles) {
		if (needDataCreazione(contentlet)) {
			setCreationDate(contentlet);
		}
		checkAllDateFieldsValue(contentlet);
		return super.checkin(contentlet, cats, permissions, user, respectFrontendRoles);
	}

	@Override
	public boolean checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user, boolean respectFrontendRoles) {
		if (needDataCreazione(contentlet)) {
			setCreationDate(contentlet);
		}
		checkAllDateFieldsValue(contentlet);
		return super.checkin(contentlet, contentRelationships, user, respectFrontendRoles);
	}

	@Override
	public boolean checkin(Contentlet contentlet, User user, boolean respectFrontendRoles, List<Category> cats) {
		if (needDataCreazione(contentlet)) {
			setCreationDate(contentlet);
		}
		checkAllDateFieldsValue(contentlet);
		return super.checkin(contentlet, user, respectFrontendRoles, cats);
	}

	@Override
	public boolean checkin(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		if (needDataCreazione(contentlet)) {
			setCreationDate(contentlet);
		}
		checkAllDateFieldsValue(contentlet);
		return super.checkin(contentlet, user, respectFrontendRoles);
	}

	@Override
	public boolean publish(Contentlet contentlet, User user, boolean respectFrontendRoles) {

		if (hasDataPubblicazioneField(contentlet)) {
			setPublishDate(contentlet);
		}

		// checkAllDateFieldsValue(contentlet);

		return super.publish(contentlet, user, respectFrontendRoles);
	}

	@Override
	public boolean publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		for (Contentlet contentlet : contentlets) {
			if (hasDataPubblicazioneField(contentlet)) {
				setPublishDate(contentlet);
			}
			checkAllDateFieldsValue(contentlet);
		}

		return super.publish(contentlets, user, respectFrontendRoles);
	}

	@Override
	public boolean unpublish(Contentlet contentlet, User user, boolean respectFrontendRoles) {
		if (hasDataPubblicazioneField(contentlet)) {
			contentlet.setDateProperty(dataPubblicazioneFieldName, null);
		}
		return super.unpublish(contentlet, user, respectFrontendRoles);
	}

	@Override
	public boolean unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {
		for (Contentlet contentlet : contentlets) {
			if (hasDataPubblicazioneField(contentlet)) {
				contentlet.setDateProperty(dataPubblicazioneFieldName, null);
			}
		}
		return super.unpublish(contentlets, user, respectFrontendRoles);
	}

	private void setPublishDate(Contentlet contentlet) {
		Date actual = new Date();
		if (contentlet.getDateProperty(dataPubblicazioneFieldName) == null
				|| (contentlet.getDateProperty(dataPubblicazioneFieldName) != null && actual.compareTo(contentlet.getDateProperty(dataPubblicazioneFieldName)) != 0)) {
			contentlet.setDateProperty(dataPubblicazioneFieldName, actual);
		}
	}

	private void setCreationDate(Contentlet contentlet) {
		contentlet.setDateProperty(dataCreazioneFieldName, new Date());
	}

	private boolean hasDataPubblicazioneField(Contentlet contentlet) {
		if (contentlet != null) {
			String stName = contentlet.getStructure().getVelocityVarName();
			if (!stName.equalsIgnoreCase("Cambio")) {
				Field f = contentlet.getStructure().getFieldVar(dataPubblicazioneFieldName);
				return f != null && UtilMethods.isSet(f.getInode());
			}
		}
		return false;
	}

	private void checkAllDateFieldsValue(Contentlet contentlet) {
		// Field changedDate =
		// contentlet.getStructure().getFieldVar(changedDateFieldName);
		// if (changedDate != null && UtilMethods.isSet(changedDate.getInode()))
		// {
		// if(
		// UtilMethods.isSet(contentlet.getStringProperty(changedDateFieldName)
		// )){
		// StringTokenizer st = new
		// StringTokenizer(contentlet.getStringProperty(changedDateFieldName),
		// ",");
		// String[] selectedDate = new String[st.countTokens()];
		//
		// int noT = 0;
		// while (st.hasMoreTokens()) {
		// selectedDate[noT] = st.nextToken();
		// noT += 1;
		// }
		//
		// for (int i = 0; i < dateToCheck.length; i++) {
		// boolean changed = true;
		// String dataI = dateToCheck[i];
		// for (int j = 0; j < selectedDate.length; j++) {
		//
		// if (selectedDate[j].equals(dataI)) {
		// changed = false;
		// break;
		// }
		// }
		// if (changed && existFieldInStructure(contentlet, dataI)){
		// contentlet.setDateProperty(dataI, null);
		// }
		// }
		// // svuto il campo delle date ogni volta che salvo la contentlet
		// contentlet.setStringProperty(changedDateFieldName, "");
		// }
		// } else if(changedDate != null &&
		// !UtilMethods.isSet(contentlet.getStringProperty(changedDateFieldName)))
		// {
		//
		// }
	}

	private boolean needDataCreazione(Contentlet contentlet) {
		boolean rValue = false;
		if (contentlet != null) {
			Field f = contentlet.getStructure().getFieldVar(dataCreazioneFieldName);
			if (f != null && UtilMethods.isSet(f.getInode())) {
				rValue = contentlet.getDateProperty(dataCreazioneFieldName) == null;
			}
			return rValue;
		}
		return rValue;
	}

	private boolean isStructureType(Contentlet contentlet, String structureName) {
		return (contentlet != null && contentlet.getStructure().getVelocityVarName().equalsIgnoreCase(structureName));
	}

	// private boolean existFieldInStructure(Contentlet c, String
	// fieldVelocityVarName) {
	// Field f = c.getStructure().getFieldVar(fieldVelocityVarName);
	// return (f != null && UtilMethods.isSet(f.getInode()));
	//
	// }

	private boolean getLinkRelated(Contentlet contentlet) {
		String q = "";
		q = "+structureName:Link +Link.allegatoId:" + contentlet.getIdentifier();
		System.out.println("Query Lucene: " + q);
		List<Contentlet> related;
		try {
			related = APILocator.getContentletAPI().search(q, -1, 0, null, APILocator.getUserAPI().getSystemUser(), false);
			System.out.println("TROVATI LINK RELAZIONATI");
			if (related.size() > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
			return false;
		}
	}

	@Override
	public boolean getMostViewedContent(String structureVariableName, String startDate, String endDate, User user) {
		return false;// TODO approfondire cosa bisogna restituire, possibile
						// override di una non meglio specificata funzione...
	}

}
