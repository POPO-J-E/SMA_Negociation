package madkit.marketorg.negociation;

import madkit.marketorg.model.Offer;
import madkit.marketorg.model.Request;

/**
 * Created by kifkif on 06/12/2017.
 */
public interface NegociationInterface {
    Offer getNewOffer(String competence, String provider);
    Offer updateNewOffer(Request request, Offer lastOffer, String competence, String provider);
}
