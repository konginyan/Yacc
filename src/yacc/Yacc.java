package yacc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Yacc {
    private static List<String> list;//每行语句
    private static List<String> notend;//非终结符号

    private static Map<String,ArrayList<ArrayList<String>>> thelist;//切换后
    private static Map<String,ArrayList<String>> firstlist;//first列表
    private static Map<String,ArrayList<String>> followlist;//first列表

    private static Map<String,Map<String,ArrayList<String>>> analysisTable;//LL1分析表

    private static int error;//错误：1左递归，2左公因子

    public static void main(String[] args) throws Exception{
        list = new ArrayList<String>();
        notend = new ArrayList<String>();
        thelist = new HashMap<String,ArrayList<ArrayList<String>>>();
        firstlist = new HashMap<String,ArrayList<String>>();
        followlist = new HashMap<String,ArrayList<String>>();
        analysisTable = new HashMap<String,Map<String,ArrayList<String>>>();

        String root = args[0];
        String file = root + "/input.bnf";
        BufferedReader f=new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
        String s;
        for (s=f.readLine( ); s!=null; s=f.readLine( )){
            if(checkRex(s)){
                list.add(s);
            }
            else{
                System.out.println("不符合BNF文法");
                return;
            }
            //System.out.println(s);
            //System.out.println(checkRex(s));
        }
        f.close();

        findNotEnd();
        toTheList();
        findFirst();
//        showFirst();
        if(error==0){
            findFollow();
        }
//        System.out.println();
//        showFollow();
//        System.out.println();
        if(checkLL1()){
            System.out.println("符合LL1文法");
            buildAnalysisTable();
//            showAnalysisTable();
//            System.out.println();
            File folder = new File(args[0]);
            String [] fileList = folder.list();
            for(int i=0;i<fileList.length;i++){
                if(fileList[i].contains("tok")){
                    if(analysisByTable(root+"/"+fileList[i])){
                        System.out.println("token"+i+"符合当前文法");
                    }
                    else {
                        System.out.println("token"+i+"不符合当前文法");
                    }
                }
            }
        }
        else System.out.println("不符合LL1文法");
    }

    //通过LL1分析表判断TOK文件是否符合当前文法
    private static boolean analysisByTable(String path) {
        Stack<String> stack = new Stack<String>();
        stack.push(notend.get(0));
        try{
            String file = path;
            BufferedReader f=new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
            String s;
            for (s=f.readLine( ); s!=null; s=f.readLine( )){
                while (true){
                    if (stack.empty())return false;
                    if(s.equals(stack.peek())){
                        stack.pop();
                        break;
                    }
                    Map<String,ArrayList<String>> map = analysisTable.get(stack.peek());
                    ArrayList<String> array = map.get(s);
                    if(array==null){
                        return false;
                    }
                    else {
                        stack.pop();
                        for(int i=array.size()-1;i>=0;i--){
                            if(!array.get(i).equals("\"\"")){
                                stack.push(array.get(i));
                            }
                        }
                    }
                }
            }
            f.close();
            while (!stack.empty()){
                Map<String,ArrayList<String>> map = analysisTable.get(stack.peek());
                ArrayList<String> array = map.get("$");
                if(array==null){
                    return false;
                }
                else {
                    stack.pop();
                    for(int i=array.size()-1;i>=0;i--){
                        if(!array.get(i).equals("\"\"")){
                            stack.push(array.get(i));
                        }
                    }
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return true;
    }

    //拆分一行
    private static void toTheList() {
        for(int i=0;i<list.size();i++){
            String[] l = list.get(i).split("::=");
            //System.out.println(l[0]+"   "+l[1]);

            String[] l1 = l[1].split("\\|");
            //System.out.println(l1.length);

            ArrayList<ArrayList<String>> group = new ArrayList<ArrayList<String>>();

            for(int j=0;j<l1.length;j++){
                ArrayList<String> unit = new ArrayList<String>();
                boolean scan = false;
                String temp = "";
                for(int k=0;k<l1[j].length();k++){
                    if(!scan){
                        if(l1[j].charAt(k)=='<'||l1[j].charAt(k)=='"') {
                            scan = !scan;
                            temp += l1[j].charAt(k);
                        }
                    }
                    else {
                        temp += l1[j].charAt(k);
                        if(l1[j].charAt(k)=='"'||l1[j].charAt(k)=='>'){
                            scan = !scan;
                            //System.out.println(temp);
                            unit.add(temp);
                            temp = "";
                        }
                    }
                }
                group.add(unit);
            }
            //System.out.println(l[0].trim() + " " + group.size());
            thelist.put(l[0].trim(),group);
        }
    }

    //算出整个first列表
    private static void findFirst(){
        error = 0;
        for(int i=0;i<notend.size();i++){
            if(error==0){
                ArrayList<String>tt = findFirst(new ArrayList<String>(),notend.get(i));
                tt = removeSame(tt);
                firstlist.put(notend.get(i),tt);
            }
        }
    }

    //算出一行first列表
    private static ArrayList<String> findFirst(ArrayList<String> root, String leaf){
        root.add(leaf);
        ArrayList<ArrayList<String>> group = thelist.get(leaf);
        ArrayList<String> first = new ArrayList<String>();
        //System.out.println(group.size());
        for(int i=0;i<group.size();i++){
            ArrayList<String> unit = group.get(i);
            for(int j=0;j<unit.size();j++){
                if(inNotEnd(unit.get(j))){
                    if(root.contains(unit.get(j))){
                        //System.out.println("存在左递归");
                        error = 1;
                        break;
                    }
                    ArrayList<String> interfirst = findFirst(root,unit.get(j));
                    boolean hasNull = false;
                    for(int k=0;k<interfirst.size();k++){
                        if(interfirst.get(k).equals("\"\""))hasNull = true;
                        first.add(interfirst.get(k));
                    }
                    if(hasNull)continue;
                    else break;
                }
                else {
                    first.add(unit.get(j));
                    break;
                }
            }
        }
        return first;
    }

    //算出所有follow列表
    private static void findFollow(){
        ArrayList<ArrayList<String>> templist = new ArrayList<ArrayList<String>>();
        for(int i=0;i<notend.size();i++){
            ArrayList<String> tempunit = new ArrayList<String>();
            templist.add(tempunit);
        }
        templist.get(0).add("$");
        //A->aBb
        for(int i=0;i<notend.size();i++){
            ArrayList<ArrayList<String>> group = thelist.get(notend.get(i));
            for(int j=0;j<group.size();j++){
                ArrayList<String> unit = group.get(j);
                for(int k=0;k<unit.size()-1;k++){
                    if(inNotEnd(unit.get(k))){
                        int n = 1;
                        boolean flag;
                        do {
                            flag = false;
                            if(inNotEnd(unit.get(k+n))){
                                ArrayList<String> strlist = firstlist.get(unit.get(k+n));
                                for(int m=0;m<strlist.size();m++){
                                    if(strlist.get(m).equals("\"\"")){
                                        flag = true;
                                    }
                                    else {
                                        templist.get(getNotEndPosition(unit.get(k)))
                                                .add(strlist.get(m));
                                    }
                                }
                                n++;
                            }
                            else {
                                templist.get(getNotEndPosition(unit.get(k))).add(unit.get(k+n));
                            }
                        }while (flag&&((k+n)<unit.size()));
                    }
                }
            }
        }
        //A->aB
        for(int i=0;i<notend.size();i++){
            ArrayList<ArrayList<String>> group = thelist.get(notend.get(i));
            for(int j=0;j<group.size();j++){
                ArrayList<String> unit = group.get(j);
                int n = 1;
                boolean flag;
                do {
                    flag = false;
                    if(inNotEnd(unit.get(unit.size()-n))){
                        if(firstlist.get(unit.get(unit.size()-n)).contains("\"\""))
                            flag = true;
                        if(!unit.get(unit.size()-n).equals(notend.get(i))){
                            templist.get(getNotEndPosition(unit.get(unit.size()-n)))
                                    .addAll(templist.get(getNotEndPosition(notend.get(i))));
                        }
                        n++;
                    }
                }while (flag&&((unit.size()-n)>=0));
            }
        }
        //统合
        for(int i=0;i<notend.size();i++){
            followlist.put(notend.get(i),removeSame(templist.get(i)));
        }
    }

    //检测bnf文法
    private static boolean checkRex(String s){
        return s.matches("<.+>\\s*::=\\s*((<.+>\\s*)|(\".*\"\\s*))+(\\|\\s*((<.+>\\s*)|(\".*\"\\s*))+)*");
    }

    //检测非终结符号
    private static boolean inNotEnd(String s){
        for(int i=0;i<notend.size();i++){
            if(s.equals(notend.get(i))){
                return true;
            }
        }
        return false;
    }

    //找出非终结符号位置
    private static int getNotEndPosition(String s){
        for(int i=0;i<notend.size();i++){
            if(s.equals(notend.get(i))){
                return i;
            }
        }
        return -1;
    }

    //找出非终结符号
    private static void findNotEnd(){
        for(int i=0;i<list.size();i++){
            String temp = "";
            for(int j=0;j<list.get(i).length();j++){
                temp += list.get(i).charAt(j);
                if(list.get(i).charAt(j)=='>'){
                    break;
                }
            }
            notend.add(temp);
            //System.out.println(temp);
        }
    }

    //检测是否LL1文法
    private static boolean checkLL1(){
        if(error != 0){
            return false;
        }
        //规则1
        for(int i=0;i<notend.size();i++){
            ArrayList<ArrayList<String>> tempgroup = new ArrayList<ArrayList<String>>();
            ArrayList<ArrayList<String>> group = thelist.get(notend.get(i));
            for(int j=0;j<group.size();j++){
                ArrayList<String> unit = group.get(j);
                ArrayList<String> tempunit = getFirstByList(unit);
                tempgroup.add(tempunit);
            }
            for(int j=0;j<tempgroup.size()-1;j++){
                for(int k=j+1;k<tempgroup.size();k++){
                    if(k!=j){
                        ArrayList<String> f = new ArrayList<String>(tempgroup.get(j));
                        f.retainAll(tempgroup.get(k));
                        if(f.size()>0)
                            return false;
                    }
                }
            }
        }
        //规则2
        for(int i=0;i<notend.size();i++){
            ArrayList<ArrayList<String>> tempgroup = new ArrayList<ArrayList<String>>();
            ArrayList<ArrayList<String>> group = thelist.get(notend.get(i));
            for(int j=0;j<group.size();j++){
                ArrayList<String> unit = group.get(j);
                ArrayList<String> tempunit = getFirstByList(unit);
                tempgroup.add(tempunit);
            }
            for(int j=0;j<tempgroup.size();j++){
                if(tempgroup.get(j).contains("\"\"")){
                    for(int k=0;k<tempgroup.size();k++){
                        if(k!=j){
                            ArrayList<String> f = new ArrayList<String>(followlist.get(notend.get(i)));
                            f.retainAll(tempgroup.get(k));
                            if(f.size()>0){
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    //构造LL1分析表
    private static void buildAnalysisTable(){
        for(int i=0;i<notend.size();i++){
            analysisTable.put(notend.get(i),new HashMap<String,ArrayList<String>>());
        }
        for(int i=0;i<notend.size();i++) {
            ArrayList<ArrayList<String>> group = thelist.get(notend.get(i));
            for (int j = 0; j < group.size(); j++) {
                ArrayList<String> unit = group.get(j);
                ArrayList<String> tempunit = getFirstByList(unit);
                for(int k=0;k<tempunit.size();k++){
                    if(!tempunit.get(k).equals("\"\"")){
                        analysisTable.get(notend.get(i)).put(tempunit.get(k),unit);
                    }
                }
                if(tempunit.contains("\"\"")){
                    ArrayList<String> f = followlist.get(notend.get(i));
                    for(int n=0;n<f.size();n++){
                        if(!f.get(n).equals("\"\"")){
                            analysisTable.get(notend.get(i)).put(f.get(n),unit);
                        }
                    }
                }
            }
        }
    }

    //得到一个没有或的列表的first
    private static ArrayList<String> getFirstByList(ArrayList<String> strlist){
        ArrayList<String> temp = new ArrayList<String>();
        for(int i=0;i<strlist.size();i++){
            if(inNotEnd(strlist.get(i))){
                temp.addAll(firstlist.get(strlist.get(i)));
                if(firstlist.get(strlist.get(i)).contains("\"\"")){
                    continue;
                }
                else break;
            }
            else{
                temp.add(strlist.get(i));
                break;
            }
        }
        return removeSame(temp);
    }

    //去除ArrayList中的重复元素
    private static ArrayList<String> removeSame(ArrayList<String> list){
        HashSet<String> set = new HashSet<String>(list);
        ArrayList<String> temp = new ArrayList<String>(set);
        return temp;
    }

    //显示first列表
    private static void showFirst(){
        System.out.println("first: ");
        for(int i=0;i<firstlist.size();i++){
            ArrayList<String> first = firstlist.get(notend.get(i));
            System.out.print(notend.get(i)+": ");
            for(int j=0;j<first.size();j++){
                System.out.print(first.get(j)+" ");
            }
            System.out.println();
        }
    }

    //显示follow列表
    private static void showFollow(){
        System.out.println("follow: ");
        for(int i=0;i<followlist.size();i++){
            ArrayList<String> follow = followlist.get(notend.get(i));
            System.out.print(notend.get(i)+": ");
            for(int j=0;j<follow.size();j++){
                System.out.print(follow.get(j)+" ");
            }
            System.out.println();
        }
    }

    //显示LL1分析表
    private static void showAnalysisTable(){
        System.out.println("analysis table: ");
        for(int i=0;i<notend.size();i++){
            System.out.print(notend.get(i)+": ");
            Map<String,ArrayList<String>> map = analysisTable.get(notend.get(i));
            Set<String> set = map.keySet();
            Iterator iter = set.iterator();
            while (iter.hasNext()) {
                System.out.println();
                String key = (String) iter.next();
                System.out.print(key+" -> ");
                ArrayList<String> temp = map.get(key);
                for(int j=0;j<temp.size();j++){
                    System.out.print(temp.get(j)+" ");
                }
            }
            System.out.println();
        }
    }
}
