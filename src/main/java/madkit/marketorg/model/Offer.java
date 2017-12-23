package madkit.marketorg.model;

import java.util.Date;

/**
 * Created by kifkif on 06/11/2017.
 */
public class Offer {

    private String type;
    private Integer price;
    private Date date;
    private String provider;

    public Offer(String type, Integer price, Date date, String provider) {
        this.type = type;
        this.price = price;
        this.date = date;
        this.provider = provider;
    }

    public String getType() {
        return type;
    }

    public Integer getPrice() {
        return price;
    }

    public Date getDate() {
        return date;
    }

    public String getProvider() {
        return provider;
    }
}
