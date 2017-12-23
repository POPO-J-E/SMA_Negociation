package madkit.marketorg.negociation;

import madkit.marketorg.model.Offer;

import java.util.Date;

/**
 * Created by kifkif on 06/12/2017.
 */
public abstract class AbstractNegoctiation implements NegociationInterface {

    public Offer getNewOffer(String competence, String provider)
    {
        return new Offer(competence, (int) (Math.random() * 500), new Date(), provider);
    }
}
