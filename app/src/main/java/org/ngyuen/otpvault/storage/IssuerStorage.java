//package org.ngyuen.otpvault.storage;
//
//import android.content.SharedPreferences;
//
//import com.google.gson.Gson;
//
//import org.ngyuen.otpvault.common.Constants;
//import org.ngyuen.otpvault.OTPVaultApplication;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.LinkedList;
//import java.util.List;
//
//public class IssuerStorage {
//    private OTPVaultApplication application;
//    private List<String> issuers = null;//- Array of labels.
//
//    public IssuerStorage(OTPVaultApplication application, Gson gson){
//        this.application = application;
//        this.loadIssuers();
//    }
//
//    private List<String> loadIssuers() {
//        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
//        if (sharedPreferences != null){
//            String str = sharedPreferences.getString(Constants.LST_ISSUERS, "");
//            if (!str.isEmpty())
//                this.issuers = new ArrayList<>(Arrays.asList(str.split(",")));
//            else
//                this.issuers = new LinkedList<String>();
//            return this.issuers;
//        } else {
//            return new ArrayList<String>();
//        }
//    }
//
//    private void updateIssuers(List<String> lstIssuers) {
//        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
//        if (sharedPreferences != null) {
//            this.issuers = lstIssuers;
//            String str = String.join(",", this.issuers);
//            sharedPreferences.edit().putString(Constants.LST_ISSUERS, str).apply();
//        }
//    }
//
//
//    private List<String> getIssuerTokenIndex(String issuer){
//        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
//        if (sharedPreferences != null) {
//            String str = sharedPreferences.getString(Constants.ISSUER_PREFIX_KEY + issuer, "");
//            if (!str.isEmpty())
//                return new ArrayList<>(Arrays.asList(str.split(",")));
//            else
//                return new ArrayList<>();
//        } else {
//            return new ArrayList<>();
//        }
//    }
//
//    private void updateIssuerTokenIndex(String issuer, List<String> issuerTokenIndex){
//        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
//        if (sharedPreferences != null) {
//            sharedPreferences.edit().putString(Constants.ISSUER_PREFIX_KEY + issuer,
//                    String.join(",", issuerTokenIndex)).apply();
//        }
//    }
//
//    private void removeIssuerTokenIndex(String issuer){
//        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
//        if (sharedPreferences != null) {
//            sharedPreferences.edit().remove(Constants.ISSUER_PREFIX_KEY + issuer).apply();
//        }
//
//    }
//
//    public void addTokenKeyOnIssuerIndex(String issuer, String key){
//        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
//        if (sharedPreferences != null) {
//            if (!this.issuers.contains(issuer)) {//- new issuer, add it to LstIssuer
//                this.issuers.add(issuer);
//                this.updateIssuers(this.issuers);
//            }
//            List<String> issuerTokenIndex = this.getIssuerTokenIndex(issuer);
//            issuerTokenIndex.add(key);
//            this.updateIssuerTokenIndex(issuer, issuerTokenIndex);
//        }
//    }
//
//    public void removeTokenKeyOnIssuerIndex(String issuer, String key){
//        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
//        if (sharedPreferences != null) {
//            //- remove token id in old issuerTokenIndex.
//            List<String> issuerTokenIndex = this.getIssuerTokenIndex(issuer);
//            issuerTokenIndex.remove(key);
//
//            if (issuerTokenIndex.size() <= 0) {//- remove the issuer from all index. Because there is no token related to this issuer.
//                this.removeIssuerTokenIndex(issuer);
//                this.issuers.remove(issuer);
//                this.updateIssuers(this.issuers);
//            } else {
//                this.updateIssuerTokenIndex(issuer, issuerTokenIndex);
//            }
//        }
//    }
//
//    public void move(String issuer, int fromPosition, int toPosition){
//        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
//        if (sharedPreferences != null) {
//            List<String> issuerTokenIndex = this.getIssuerTokenIndex(issuer);
//
//            if (fromPosition == toPosition)
//                return;
//
//            if (fromPosition < 0 || fromPosition > issuerTokenIndex.size())
//                return;
//            if (toPosition < 0 || toPosition > issuerTokenIndex.size())
//                return;
//
//            issuerTokenIndex.add(toPosition, issuerTokenIndex.remove(fromPosition));
//            this.updateIssuerTokenIndex(issuer, issuerTokenIndex);
//        }
//    }
//
//    public String[] getIssuers(){
//        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
//        if (sharedPreferences != null) {
//            return this.issuers.toArray(new String[this.issuers.size()]);
//        } else {
//            return new String[0];
//        }
//    }
//
//    public int getIssuerTokenIndexLength(String issuer){
//        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
//        if (sharedPreferences != null) {
//            List<String> index = this.getIssuerTokenIndex(issuer);
//            if (index == null)
//                return 0;
//            else
//                return index.size();
//        } else {
//            return 0;
//        }
//    }
//
//    public String getKey(String issuer, int position){
//        SharedPreferences sharedPreferences = this.application.getSharedPreferencesStorage();
//        if (sharedPreferences != null) {
//            List<String> index = this.getIssuerTokenIndex(issuer);
//            if (position < 0 || position >= index.size())
//                return null;
//            else
//                return index.get(position);
//        } else {
//            return null;
//        }
//    }
//}
