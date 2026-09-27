package com.channak.billinvoicepro;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    LinearLayout root, itemsBox;
    EditText billTo, invoiceNo, date;
    ArrayList<EditText[]> rows = new ArrayList<>();
    int rowCount = 0;
    String[] descs = new String[4];

    static final String PREF = "invoice_history";

    int dp(float n){ return (int)(n*getResources().getDisplayMetrics().density + .5f); }
    TextView tv(String s, int sp, boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.rgb(25,25,25));
        if(bold) t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setPadding(dp(6),dp(4),dp(6),dp(4)); return t;
    }
    EditText edit(String hint){
        EditText e=new EditText(this); e.setHint(hint); e.setTextSize(14); e.setSingleLine(true);
        e.setPadding(dp(8),dp(4),dp(8),dp(4)); return e;
    }
    Button btn(String text){
        Button b=new Button(this); b.setText(text); b.setAllCaps(false); return b;
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        buildUI();
    }

    void buildUI(){
        ScrollView scroll=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12),dp(10),dp(12),dp(20));
        scroll.addView(root);

        TextView title=tv("CHannak Bill Invoice Pro",24,true); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView sub=tv("VIKAS ENTERPRISES • Offline Billing",14,false); sub.setGravity(Gravity.CENTER); root.addView(sub);

        LinearLayout company=new LinearLayout(this); company.setOrientation(LinearLayout.VERTICAL);
        company.setPadding(dp(12),dp(10),dp(12),dp(10));
        company.setBackgroundColor(Color.rgb(245,247,250));
        TextView c1=tv("VIKAS ENTERPRISES",20,true); c1.setGravity(Gravity.CENTER); company.addView(c1);
        company.addView(tv("110, 2nd Floor, D-8, Shriram Complex,\nOpp. Multipurpose School, Gumanpura,\nKota - 324007\nGST No.: 08BJ4PP2710ZN   |   Udyam: UDYAM-RJ-24-0108180\nMob.: 86193-86311   |   Email: dharam.prajapat10823@gmail.com",13,false));
        root.addView(company);

        billTo=edit("Bill To / Customer Name & Address"); root.addView(billTo);
        LinearLayout meta=new LinearLayout(this); meta.setOrientation(LinearLayout.HORIZONTAL);
        invoiceNo=edit("Invoice No."); date=edit("Date");
        meta.addView(invoiceNo,new LinearLayout.LayoutParams(0,dp(52),1));
        meta.addView(date,new LinearLayout.LayoutParams(0,dp(52),1));
        root.addView(meta);

        LinearLayout head=new LinearLayout(this);
        String[] hs={"S.No.","Description","Qty","Rate","Amount"};
        int[] weights={.65f,2.7f,.65f,1f,1.1f};
        for(int i=0;i<hs.length;i++){ TextView h=tv(hs[i],13,true); h.setGravity(Gravity.CENTER); head.addView(h,new LinearLayout.LayoutParams(0,dp(48),weights[i]));}
        root.addView(head);
        itemsBox=new LinearLayout(this); itemsBox.setOrientation(LinearLayout.VERTICAL); root.addView(itemsBox);
        for(int i=0;i<4;i++) addRow();

        TextView total=tv("SUB TOTAL: ₹ 0.00",17,true); total.setGravity(Gravity.RIGHT); total.setId(9001); root.addView(total);
        TextView words=tv("Amount in Words: —",14,true); words.setId(9002); root.addView(words);

        LinearLayout actions=new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button add=btn("+ Add Item"); add.setOnClickListener(v->addRow());
        Button save=btn("Save Bill"); save.setOnClickListener(v->saveBill());
        Button pdf=btn("Create A4 PDF"); pdf.setOnClickListener(v->createPdf());
        actions.addView(add,new LinearLayout.LayoutParams(0,dp(55),1));
        actions.addView(save,new LinearLayout.LayoutParams(0,dp(55),1));
        actions.addView(pdf,new LinearLayout.LayoutParams(0,dp(55),1));
        root.addView(actions);

        Button history=btn("Invoice History"); history.setOnClickListener(v->showHistory());
        root.addView(history);

        root.addView(tv("OUR BANK\nCENTRAL BANK OF INDIA\n(Jhalawar Road, Kota)\nIFSC: CBIN0281016\nA/C: 5145880526\n\nTerms & Conditions\n1. Payment should be made 7 days.\n2. Subject to Kota jurisdiction.\n3. Payment should be made in favour of Vikas Enterprises.\n4. E. & O.E.\n\n                         Authorized Signature",13,false));

        setContentView(scroll);
        invoiceNo.setText(nextInvoice());
        date.setText(new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(new Date()));
    }

    void addRow(){
        if(rowCount>=10) return;
        rowCount++;
        LinearLayout line=new LinearLayout(this);
        EditText d=edit("Description"); EditText q=edit("Qty"); EditText r=edit("Rate"); EditText a=edit("Amount");
        a.setEnabled(false);
        TextView n=tv(String.valueOf(rowCount)+".",13,false); n.setGravity(Gravity.CENTER);
        float[] w={.65f,2.7f,.65f,1f,1.1f};
        line.addView(n,new LinearLayout.LayoutParams(0,dp(52),w[0]));
        line.addView(d,new LinearLayout.LayoutParams(0,dp(52),w[1]));
        line.addView(q,new LinearLayout.LayoutParams(0,dp(52),w[2]));
        line.addView(r,new LinearLayout.LayoutParams(0,dp(52),w[3]));
        line.addView(a,new LinearLayout.LayoutParams(0,dp(52),w[4]));
        View.OnFocusChangeListener f=(v,has)->{ if(!has) recalc(); };
        q.setOnFocusChangeListener(f); r.setOnFocusChangeListener(f);
        itemsBox.addView(line); rows.add(new EditText[]{d,q,r,a});
    }

    double subtotal(){
        double sum=0;
        for(EditText[] x:rows){
            try{
                double q=Double.parseDouble(x[1].getText().toString());
                double r=Double.parseDouble(x[2].getText().toString());
                double a=q*r; x[3].setText(String.format(Locale.US,"%.2f",a)); sum+=a;
            }catch(Exception e){}
        }
        return sum;
    }
    void recalc(){
        double s=subtotal();
        ((TextView)findViewById(9001)).setText(String.format(Locale.US,"SUB TOTAL: ₹ %.2f",s));
        ((TextView)findViewById(9002)).setText("Amount in Words: "+numberToWords((long)Math.round(s))+" Rupees Only");
    }

    String nextInvoice(){
        int n=getSharedPreferences(PREF,0).getInt("next",1);
        return "CB/"+new SimpleDateFormat("yyyy",Locale.getDefault()).format(new Date())+"/"+n;
    }

    void saveBill(){
        recalc();
        String key=invoiceNo.getText().toString()+" | "+date.getText().toString()+" | "+billTo.getText().toString()+" | ₹"+String.format(Locale.US,"%.2f",subtotal());
        getSharedPreferences(PREF,0).edit().putString("bill_"+System.currentTimeMillis(),key)
            .putInt("next",getSharedPreferences(PREF,0).getInt("next",1)+1).apply();
        Toast.makeText(this,"Bill saved successfully",Toast.LENGTH_SHORT).show();
        invoiceNo.setText(nextInvoice());
    }

    void showHistory(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(10),dp(20),dp(10));
        SharedPreferences p=getSharedPreferences(PREF,0);
        boolean any=false;
        for(String k:p.getAll().keySet()) if(k.startsWith("bill_")){ any=true; box.addView(tv(String.valueOf(p.getAll().get(k)),14,false)); }
        if(!any) box.addView(tv("No saved invoices yet.",16,false));
        new AlertDialog.Builder(this).setTitle("Invoice History").setView(box).setPositiveButton("Close",null).show();
    }

    void createPdf(){
        recalc();
        PdfDocument doc=new PdfDocument();
        PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(595,842,1).create();
        PdfDocument.Page page=doc.startPage(info); Canvas c=page.getCanvas(); Paint p=new Paint(1);
        p.setColor(Color.BLACK); p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.NORMAL));
        float x=42,y=42;
        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(18); c.drawText("INVOICE",255,y,p);
        p.setTextSize(9); p.setTypeface(Typeface.DEFAULT); c.drawText("ORIGINAL COPY",42,68,p);
        c.drawText("GST No.: 08BJ4PP2710ZN",42,92,p); c.drawText("Udyam: UDYAM-RJ-24-0108180",42,108,p); c.drawText("Mob.: 86193-86311",445,92,p);
        p.setStyle(Paint.Style.STROKE); c.drawRect(42,125,553,215,p); p.setStyle(Paint.Style.FILL);
        p.setTextSize(17); p.setTypeface(Typeface.DEFAULT_BOLD); c.drawText("VIKAS ENTERPRISES",207,148,p);
        p.setTextSize(9); p.setTypeface(Typeface.DEFAULT);
        c.drawText("110, 2nd Floor, D-8, Shriram Complex,",50,170,p); c.drawText("Opp. Multipurpose School, Gumanpura, Kota - 324007",50,184,p);
        c.drawText("Email: dharam.prajapat10823@gmail.com",50,198,p);
        p.setStyle(Paint.Style.STROKE); c.drawRect(42,230,553,290,p); p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.DEFAULT_BOLD); c.drawText("To:",50,248,p); c.drawText("Invoice No.: "+invoiceNo.getText(),350,248,p); c.drawText("Date: "+date.getText(),350,270,p);
        p.setStyle(Paint.Style.STROKE); c.drawRect(42,305,553,525,p); p.setStyle(Paint.Style.FILL);
        float[] cols={42,88,350,405,480,553};
        for(float xx:cols)c.drawLine(xx,305,xx,525,p);
        for(int i=0;i<=5;i++)c.drawLine(42,305+i*36,553,305+i*36,p);
        p.setTypeface(Typeface.DEFAULT_BOLD); c.drawText("S. No.",48,327,p); c.drawText("Description",94,327,p); c.drawText("Qty.",355,327,p); c.drawText("Rate",412,327,p); c.drawText("Amount",487,327,p);
        p.setTypeface(Typeface.DEFAULT);
        int yy=363; int idx=1;
        for(EditText[] z:rows){
            if(z[0].getText().length()>0){
                c.drawText(""+idx,57,yy,p); c.drawText(z[0].getText().toString(),94,yy,p); c.drawText(z[1].getText().toString(),358,yy,p); c.drawText(z[2].getText().toString(),412,yy,p); c.drawText(z[3].getText().toString(),487,yy,p); idx++;
            }
            yy+=36; if(yy>490) break;
        }
        double s=subtotal(); p.setTypeface(Typeface.DEFAULT_BOLD); c.drawText("SUB TOTAL",410,510,p); c.drawText(String.format(Locale.US,"%.2f",s),487,510,p);
        p.setStyle(Paint.Style.STROKE); c.drawRect(42,540,553,578,p); p.setStyle(Paint.Style.FILL); c.drawText("Amount in Words: "+numberToWords((long)Math.round(s))+" Rupees Only",50,564,p);
        p.setStyle(Paint.Style.STROKE); c.drawRect(42,595,300,705,p); p.setStyle(Paint.Style.FILL); p.setTypeface(Typeface.DEFAULT_BOLD); c.drawText("OUR BANK",50,615,p);
        p.setTypeface(Typeface.DEFAULT); c.drawText("CENTRAL BANK OF INDIA",50,633,p); c.drawText("(Jhalawar Road, Kota)",50,648,p); c.drawText("IFSC: CBIN0281016",50,664,p); c.drawText("A/C: 5145880526",50,680,p);
        p.setTypeface(Typeface.DEFAULT_BOLD); c.drawText("Terms & Conditions",315,615,p); p.setTypeface(Typeface.DEFAULT); c.drawText("1. Payment should be made 7 days.",315,632,p); c.drawText("2. Subject to Kota jurisdiction.",315,648,p); c.drawText("3. Payment in favour of Vikas Enterprises.",315,664,p); c.drawText("4. E. & O.E.",315,680,p);
        p.setTypeface(Typeface.DEFAULT_BOLD); c.drawText("Authorized Signature",447,728,p);
        doc.finishPage(page);
        try{
            File dir=new File(getExternalFilesDir(null),"Invoices"); dir.mkdirs();
            File f=new File(dir,invoiceNo.getText().toString().replace("/","_")+".pdf");
            FileOutputStream out=new FileOutputStream(f); doc.writeTo(out); out.close(); doc.close();
            Intent share=new Intent(Intent.ACTION_SEND); share.setType("application/pdf"); share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
            startActivity(Intent.createChooser(share,"Share Invoice PDF"));
        }catch(Exception e){ Toast.makeText(this,"PDF error: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }

    String numberToWords(long n){
        if(n==0)return"Zero";
        String[] one={"","One","Two","Three","Four","Five","Six","Seven","Eight","Nine","Ten","Eleven","Twelve","Thirteen","Fourteen","Fifteen","Sixteen","Seventeen","Eighteen","Nineteen"};
        String[] ten={"","","Twenty","Thirty","Forty","Fifty","Sixty","Seventy","Eighty","Ninety"};
        if(n<20)return one[(int)n];
        if(n<100)return ten[(int)n/10]+(n%10==0?"":" "+one[(int)n%10]);
        if(n<1000)return one[(int)n/100]+" Hundred"+(n%100==0?"":" "+numberToWords(n%100));
        if(n<100000)return numberToWords(n/1000)+" Thousand"+(n%1000==0?"":" "+numberToWords(n%1000));
        if(n<10000000)return numberToWords(n/100000)+" Lakh"+(n%100000==0?"":" "+numberToWords(n%100000));
        return numberToWords(n/10000000)+" Crore"+(n%10000000==0?"":" "+numberToWords(n%10000000));
    }
}
