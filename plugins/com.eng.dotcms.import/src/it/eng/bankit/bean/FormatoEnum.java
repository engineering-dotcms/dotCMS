package it.eng.bankit.bean;

public enum FormatoEnum {

	I1("I1"), I3("I3"), I2("I2"), D1("D1"), D2("D2") , D3("D3");
	 
	 private String formato;
	 
	 private FormatoEnum(String format) {
		 formato = format;
	 }
	 
	 public String getFormato() {
	   return formato;
	 }
	 
	 
	 
}
