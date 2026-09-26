package shop.kalano.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
  private static final String SITE="https://kalano.shop";
  private final int GOLD=Color.rgb(255,184,0), DARK=Color.rgb(39,39,39), LIGHT=Color.rgb(247,248,250);
  private final ExecutorService executor=Executors.newFixedThreadPool(5);
  private LinearLayout content, slider, products;
  private TextView status;
  private final ArrayList<JSONObject> banners=new ArrayList<>();
  private int bannerIndex=0;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private final Runnable rotate=new Runnable(){public void run(){if(banners.size()>1){bannerIndex=(bannerIndex+1)%banners.size();showBanner();}handler.postDelayed(this,4500);}};
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.WHITE);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);splash();handler.postDelayed(this::home,1650);}
  @Override protected void onDestroy(){handler.removeCallbacks(rotate);executor.shutdownNow();super.onDestroy();}
  private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
  private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);if(bold)t.setTypeface(null,1);return t;}
  private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
  private void splash(){LinearLayout l=column();l.setGravity(Gravity.CENTER);l.setBackgroundColor(Color.WHITE);ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.kalano_logo);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);l.addView(logo,new LinearLayout.LayoutParams(-1,dp(300)));ProgressBar p=new ProgressBar(this);p.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(GOLD));l.addView(p,new LinearLayout.LayoutParams(dp(35),dp(35)));setContentView(l);}
  private void home(){LinearLayout root=column();root.setBackgroundColor(LIGHT);LinearLayout top=column();top.setPadding(dp(14),dp(8),dp(14),dp(10));top.setBackgroundColor(Color.WHITE);TextView brand=text("کالانو",26,DARK,true);brand.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);top.addView(brand,new LinearLayout.LayoutParams(-1,dp(46)));EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);search.setHint("جستجو در کالانو 🔎");search.setPadding(dp(15),0,dp(15),0);search.setBackground(bg(LIGHT,18));top.addView(search,new LinearLayout.LayoutParams(-1,dp(49)));search.setOnEditorActionListener((v,a,e)->{open(SITE+"/?s="+Uri.encode(search.getText().toString())+"&post_type=product");return true;});root.addView(top);
    ScrollView scroll=new ScrollView(this);content=column();content.setPadding(dp(10),dp(12),dp(10),dp(10));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    slider=column();slider.setBackground(bg(Color.WHITE,18));slider.setGravity(Gravity.CENTER);TextView loading=text("در حال دریافت بنرها...",15,DARK,false);slider.addView(loading,new LinearLayout.LayoutParams(-1,dp(190)));content.addView(slider,new LinearLayout.LayoutParams(-1,dp(205)));
    content.addView(text("دسته‌بندی‌های کالانو",19,DARK,true),new LinearLayout.LayoutParams(-1,dp(57)));
    HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout cats=new LinearLayout(this);cats.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);String[] names={"همه محصولات","لپ‌تاپ","لوازم جانبی","موبایل","پیشنهادها"};String[] links={"/shop/","/product-category/laptop/","/product-category/accessories/","/product-category/mobile/","/shop/"};for(int i=0;i<names.length;i++){final String link=links[i];TextView chip=text(names[i],13,DARK,true);chip.setPadding(dp(12),0,dp(12),0);chip.setBackground(bg(Color.WHITE,15));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,dp(48));cp.setMargins(dp(4),0,dp(4),0);cats.addView(chip,cp);chip.setOnClickListener(v->open(SITE+link));}hs.addView(cats);content.addView(hs);
    TextView title=text("جدیدترین محصولات",19,DARK,true);title.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);content.addView(title,new LinearLayout.LayoutParams(-1,dp(65)));
    products=column();content.addView(products);status=text("در حال دریافت محصولات...",14,DARK,false);products.addView(status,new LinearLayout.LayoutParams(-1,dp(80)));
    LinearLayout nav=new LinearLayout(this);nav.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);nav.setBackgroundColor(Color.WHITE);String[] tabs={"⌂\nخانه","▦\nدسته‌بندی","🛒\nسبد خرید","♙\nحساب من"};String[] urls={"",SITE+"/shop/",SITE+"/cart/",SITE+"/my-account/"};for(int i=0;i<tabs.length;i++){final int idx=i;TextView t=text(tabs[i],13,i==0?GOLD:DARK,true);nav.addView(t,new LinearLayout.LayoutParams(0,dp(65),1));t.setOnClickListener(v->{if(idx==0)home();else open(urls[idx]);});}root.addView(nav);setContentView(root);loadBanners();loadProducts();handler.removeCallbacks(rotate);handler.postDelayed(rotate,4500);
  }
  private String fetch(String path)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(path).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setRequestProperty("Accept","application/json");try{if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}return out.toString("UTF-8");}finally{c.disconnect();}}
  private void loadBanners(){executor.execute(()->{try{JSONArray arr=new JSONArray(fetch(SITE+"/wp-json/kalano/v1/banners"));ArrayList<JSONObject> list=new ArrayList<>();for(int i=0;i<arr.length();i++)list.add(arr.getJSONObject(i));runOnUiThread(()->{banners.clear();banners.addAll(list);bannerIndex=0;showBanner();});}catch(Exception ex){runOnUiThread(()->{slider.removeAllViews();TextView msg=text("بنرها در دسترس نیستند",14,DARK,false);slider.addView(msg,new LinearLayout.LayoutParams(-1,dp(190)));});}});}
  private void showBanner(){if(slider==null)return;slider.removeAllViews();if(banners.isEmpty()){slider.addView(text("هنوز بنری ثبت نشده",15,DARK,false),new LinearLayout.LayoutParams(-1,dp(190)));return;}JSONObject b=banners.get(bannerIndex);ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);slider.addView(im,new LinearLayout.LayoutParams(-1,dp(180)));image(im,b.optString("image"));im.setOnClickListener(v->open(safeUrl(b.optString("link"))));TextView dots=text((bannerIndex+1)+" / "+banners.size(),12,DARK,false);slider.addView(dots,new LinearLayout.LayoutParams(-1,dp(24)));}
  private String safeUrl(String s){return s!=null&&s.startsWith("https://kalano.shop/")?s:SITE;}
  private void image(ImageView im,String url){if(url==null||!url.startsWith("https://"))return;executor.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(12000);Bitmap bm;try(InputStream in=c.getInputStream()){bm=BitmapFactory.decodeStream(in);}finally{c.disconnect();}if(bm!=null)runOnUiThread(()->im.setImageBitmap(bm));}catch(Exception ignored){}});}
  private void loadProducts(){executor.execute(()->{try{JSONArray arr=new JSONArray(fetch(SITE+"/wp-json/wc/store/v1/products?per_page=20"));runOnUiThread(()->{products.removeAllViews();if(arr.length()==0){products.addView(text("محصولی پیدا نشد",14,DARK,false));return;}for(int i=0;i<arr.length();i++){JSONObject p=arr.optJSONObject(i);if(p!=null)product(p);}});}catch(Exception ex){runOnUiThread(()->{products.removeAllViews();TextView err=text("اتصال محصولات برقرار نشد. برای مشاهده فروشگاه لمس کنید.",14,DARK,false);err.setPadding(dp(10),dp(20),dp(10),dp(20));products.addView(err);err.setOnClickListener(v->open(SITE+"/shop/"));});}});}
  private void product(JSONObject p){LinearLayout card=new LinearLayout(this);card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(10),dp(10),dp(10),dp(10));card.setBackground(bg(Color.WHITE,16));LinearLayout.LayoutParams outer=new LinearLayout.LayoutParams(-1,dp(136));outer.setMargins(0,0,0,dp(9));products.addView(card,outer);ImageView photo=new ImageView(this);photo.setScaleType(ImageView.ScaleType.FIT_CENTER);card.addView(photo,new LinearLayout.LayoutParams(dp(112),dp(112)));JSONArray images=p.optJSONArray("images");if(images!=null&&images.length()>0)image(photo,images.optJSONObject(0).optString("src"));LinearLayout info=column();info.setPadding(dp(8),0,dp(8),0);card.addView(info,new LinearLayout.LayoutParams(0,-2,1));TextView name=text(android.text.Html.fromHtml(p.optString("name"),android.text.Html.FROM_HTML_MODE_LEGACY).toString(),15,DARK,true);name.setGravity(Gravity.RIGHT);name.setMaxLines(3);info.addView(name);JSONObject prices=p.optJSONObject("prices");String price="مشاهده قیمت";if(prices!=null){try{String raw=prices.optString("price");int minor=prices.optInt("currency_minor_unit",0);double n=Double.parseDouble(raw)/Math.pow(10,minor);price=NumberFormat.getNumberInstance(new Locale("fa")).format(n)+" "+prices.optString("currency_suffix",prices.optString("currency_code",""));}catch(Exception ignored){}}TextView amount=text(price,15,DARK,true);amount.setGravity(Gravity.RIGHT);amount.setPadding(0,dp(12),0,0);info.addView(amount);card.setOnClickListener(v->open(safeUrl(p.optString("permalink"))));}
  private void open(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ex){Toast.makeText(this,"مرورگر در دسترس نیست",Toast.LENGTH_SHORT).show();}}
}
