package ee.oop.onlinechat;

public class KliendiInfo {
    String nimi;
    Boolean sisselogitud;

    public KliendiInfo() {
        this.nimi = "Default";
        this.sisselogitud = false;
    }



    public String getNimi() {
        return nimi;
    }

    public void setNimi(String nimi) {
        this.nimi = nimi;
    }
}
