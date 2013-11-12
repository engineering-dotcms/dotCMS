package it.eng.dotcms.sitemap.wrapper;

public class HtmlLinkWrapper implements Comparable<HtmlLinkWrapper> {
	private String id;
	private Integer depth;
	private String label;
	private String href;
	private Integer order;
	private String absoluteOrder;
	private String path;

	public HtmlLinkWrapper(){}
	
	public HtmlLinkWrapper(String id, Integer depth, String label, String href, Integer order, String path) {
		this.id = id;
		this.depth = depth;
		this.label = label;
		this.href = href;
		this.order = order;
		this.path = path;
	}
	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	public Integer getDepth() {
		return depth;
	}
	public void setDepth(Integer depth) {
		this.depth = depth;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getHref() {
		return href;
	}
	public void setHref(String href) {
		this.href = href;
	}
	public Integer getOrder() {
		return order;
	}
	public void setOrder(Integer order) {
		this.order = order;
	}

	public String getAbsoluteOrder() {
		return absoluteOrder;
	}

	public void setAbsoluteOrder(String absoluteOrder) {
		this.absoluteOrder = absoluteOrder;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public int compareTo(HtmlLinkWrapper o) {
		if(o != null && !this.equals(o) && absoluteOrder != null && o.getAbsoluteOrder() != null) {
			if(!absoluteOrder.equals(o.getAbsoluteOrder())) {
				String [] levels = absoluteOrder.split("::");
				String [] otherLevels = o.getAbsoluteOrder().split("::");
				
//				if(levels.length != otherLevels.length)
//					return new Integer(levels.length).compareTo(new Integer(otherLevels.length));
					
				for(int ii = 0; ii < levels.length; ii++) {
					String parts[] = levels[ii].split("!!");
					if(otherLevels.length > ii) {
						String otherParts[] = otherLevels[ii].split("!!");
						if(parts != null && parts.length > 0) {
							if(parts[0] != null && !parts[0].equals("")
								&& otherParts[0] != null && !otherParts[0].equals("")) 
							{
								try {
									Integer numberPart = Integer.parseInt(parts[0]);
									Integer otherNumberPart = Integer.parseInt(otherParts[0]);
									
									if(!numberPart.equals(otherNumberPart))
										return numberPart.compareTo(otherNumberPart);
									else if(parts.length > 1 && otherParts.length > 1) {
										String stringPart = parts[1];
										String otherStringPart = otherParts[1];
										
										if(!stringPart.equals(otherStringPart))
											return stringPart.compareTo(otherStringPart);
									}
								} catch (NumberFormatException e) {}
							}
						}
					} else {
						break;
					}
				}
				
				return absoluteOrder.compareTo(o.getAbsoluteOrder());
			} else
				return href.compareTo(o.href);
		} else {
			return 0;
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HtmlLinkWrapper other = (HtmlLinkWrapper) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
