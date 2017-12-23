package madkit.marketorg.negociation;

import madkit.marketorg.Client;
import madkit.marketorg.model.Request;

/**
 * Created by kifkif on 06/12/2017.
 */
public abstract class AbstractRequestNegociation implements RequestNegociationInterface {
    public Request getNewRequest(Client client){
        return new Request(client.getProduct(), client.getPreferedCompanies(), client.getPreferedCompanies(), client.getBudgetMax(), client.getLimitDate());
    }
}
