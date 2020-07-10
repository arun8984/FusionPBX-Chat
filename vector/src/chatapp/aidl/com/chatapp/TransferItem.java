package com.chatapp;

class TransferItem {
    public String Sender;
    public String Receiver;
    public String Amount;
    public String Currency;
    public String time;

    public TransferItem(){}

    public TransferItem(String Sender, String Receiver, String Amount, String Currency, String time) {
        this.Sender = Sender;
        this.Receiver = Receiver;
        this.Amount = Amount;
        this.Currency = Currency;
        this.time = time;
    }

}
