package madkit.marketorg.model;

import java.util.Date;
import java.util.List;

/**
 * Created by kifkif on 06/11/2017.
 */
public class Request {
    private String type;
    private List<String> preferedCompanies;
    private List<String> blackedCompanies;
    private Integer budgetMax;
    private Date limitDate;

    public Request(String type, List<String> preferedCompanies, List<String> blakedCompanies, Integer budgetMax, Date limitDate) {
        this.type = type;
        this.preferedCompanies = preferedCompanies;
        this.blackedCompanies = blakedCompanies;
        this.budgetMax = budgetMax;
        this.limitDate = limitDate;
    }

    public String getType() {
        return type;
    }

    public List<String> getPreferedCompanies() {
        return preferedCompanies;
    }

    public List<String> getBlackedCompanies() {
        return blackedCompanies;
    }

    public Integer getBudgetMax() {
        return budgetMax;
    }

    public Date getLimitDate() {
        return limitDate;
    }
}
