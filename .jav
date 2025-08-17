// ===============================
// Library Management System – Pure Java (Console)
// Storage: Java Serialization (files) – no external DB/driver required
// JDK: 11+
// How to run:
//   1) Save files in a folder (same package) as shown below.
//   2) Compile:  javac *.java
//   3) Run:      java LibraryApp
// Data files will be created under ./data on first run.
// ===============================

// ───────────────────────────────────────────────
// File: Book.java
// ───────────────────────────────────────────────
import java.io.Serializable;
import java.util.Objects;

class Book implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;            // UUID-like string
    private String title;
    private String author;
    private String isbn;                // optional/unique-ish
    private int totalCopies;
    private int availableCopies;

    public Book(String id, String title, String author, String isbn, int totalCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.totalCopies = Math.max(1, totalCopies);
        this.availableCopies = this.totalCopies;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }

    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public void setTotalCopies(int totalCopies) {
        if (totalCopies < 1) return;
        int delta = totalCopies - this.totalCopies;
        this.totalCopies = totalCopies;
        this.availableCopies = Math.max(0, this.availableCopies + delta);
    }

    public boolean borrowOne() {
        if (availableCopies <= 0) return false;
        availableCopies--;
        return true;
    }

    public void returnOne() {
        if (availableCopies < totalCopies) availableCopies++;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book b = (Book) o;
        return Objects.equals(id, b.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return String.format("[%s] %s by %s | ISBN: %s | %d/%d available",
                id, title, author, isbn == null ? "-" : isbn, availableCopies, totalCopies);
    }
}

// ───────────────────────────────────────────────
// File: Member.java
// ───────────────────────────────────────────────
import java.io.Serializable;
import java.util.Objects;

class Member implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;  // UUID-like
    private String name;
    private String email;
    private String phone;

    public Member(String id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member)) return false;
        Member m = (Member) o;
        return Objects.equals(id, m.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        return String.format("[%s] %s | %s | %s", id, name, email, phone);
    }
}

// ───────────────────────────────────────────────
// File: Loan.java
// ───────────────────────────────────────────────
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

class Loan implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;       // UUID-like
    private final String bookId;
    private final String memberId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;  // null if not yet returned

    public static final int DAILY_FINE = 5; // currency units per overdue day

    public Loan(String id, String bookId, String memberId, LocalDate issueDate, LocalDate dueDate) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
    }

    public String getId() { return id; }
    public String getBookId() { return bookId; }
    public String getMemberId() { return memberId; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }

    public boolean isReturned() { return returnDate != null; }

    public void markReturned(LocalDate date) { this.returnDate = date; }

    public long daysOverdue(LocalDate today) {
        LocalDate effective = isReturned() ? returnDate : today;
        if (effective.isAfter(dueDate)) return ChronoUnit.DAYS.between(dueDate, effective);
        return 0;
    }

    public long fine(LocalDate today) {
        return daysOverdue(today) * DAILY_FINE;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Loan)) return false;
        Loan l = (Loan) o;
        return Objects.equals(id, l.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    @Override public String toString() {
        String status = isReturned() ? ("Returned on " + returnDate) : "Not returned";
        return String.format("Loan[%s] book=%s member=%s issue=%s due=%s | %s",
                id, bookId, memberId, issueDate, dueDate, status);
    }
}

// ───────────────────────────────────────────────
// File: Persistence.java
// ───────────────────────────────────────────────
import java.io.*;
import java.nio.file.*;

class Persistence {
    private final Path dataDir;

    public Persistence(String dir) {
        this.dataDir = Paths.get(dir);
    }

    public void ensureDir() {
        try { Files.createDirectories(dataDir); } catch (IOException ignored) {}
    }

    public <T> void save(String fileName, T object) throws IOException {
        ensureDir();
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(dataDir.resolve(fileName)))) {
            oos.writeObject(object);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T load(String fileName) throws IOException, ClassNotFoundException {
        Path p = dataDir.resolve(fileName);
        if (!Files.exists(p)) return null;
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(p))) {
            return (T) ois.readObject();
        }
    }
}

// ───────────────────────────────────────────────
// File: Library.java
// ───────────────────────────────────────────────
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

class Library implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Book> books = new LinkedHashMap<>();
    private final Map<String, Member> members = new LinkedHashMap<>();
    private final Map<String, Loan> loans = new LinkedHashMap<>();

    // constraints
    private final int loanDays = 14;
    private final int maxConcurrentLoansPerMember = 5;

    transient private Persistence persistence; // not serialized; reattached on load

    public void attachPersistence(Persistence p) { this.persistence = p; }

    public static String newId() { return UUID.randomUUID().toString().substring(0, 8); }

    // ── Books
    public Book addBook(String title, String author, String isbn, int copies) {
        String id = newId();
        Book b = new Book(id, title, author, isbn, copies);
        books.put(id, b);
        return b;
    }

    public boolean removeBook(String id) {
        if (!books.containsKey(id)) return false;
        // Ensure no active loans for this book
        boolean inLoan = loans.values().stream().anyMatch(l -> l.getBookId().equals(id) && !l.isReturned());
        if (inLoan) return false;
        books.remove(id);
        return true;
    }

    public Collection<Book> listBooks() { return books.values(); }

    public List<Book> searchBooks(String keyword) {
        String k = keyword.toLowerCase();
        List<Book> out = new ArrayList<>();
        for (Book b : books.values()) {
            if ((b.getTitle() != null && b.getTitle().toLowerCase().contains(k)) ||
                (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(k)) ||
                (b.getIsbn() != null && b.getIsbn().toLowerCase().contains(k))) {
                out.add(b);
            }
        }
        return out;
    }

    // ── Members
    public Member addMember(String name, String email, String phone) {
        String id = newId();
        Member m = new Member(id, name, email, phone);
        members.put(id, m);
        return m;
    }

    public boolean removeMember(String id) {
        // can't remove if member has active loans
        boolean active = loans.values().stream().anyMatch(l -> l.getMemberId().equals(id) && !l.isReturned());
        if (active) return false;
        return members.remove(id) != null;
    }

    public Collection<Member> listMembers() { return members.values(); }

    // ── Loans
    public Optional<Loan> issueBook(String bookId, String memberId) {
        Book b = books.get(bookId);
        if (b == null || b.getAvailableCopies() <= 0) return Optional.empty();

        long activeLoans = loans.values().stream().filter(l -> l.getMemberId().equals(memberId) && !l.isReturned()).count();
        if (activeLoans >= maxConcurrentLoansPerMember) return Optional.empty();

        if (!b.borrowOne()) return Optional.empty();
        LocalDate today = LocalDate.now();
        Loan loan = new Loan(newId(), bookId, memberId, today, today.plusDays(loanDays));
        loans.put(loan.getId(), loan);
        return Optional.of(loan);
    }

    public Optional<Long> returnBook(String loanId) {
        Loan loan = loans.get(loanId);
        if (loan == null || loan.isReturned()) return Optional.empty();
        loan.markReturned(LocalDate.now());
        Book b = books.get(loan.getBookId());
        if (b != null) b.returnOne();
        return Optional.of(loan.fine(LocalDate.now()));
    }

    public Collection<Loan> listLoans(boolean onlyActive) {
        if (!onlyActive) return loans.values();
        List<Loan> active = new ArrayList<>();
        for (Loan l : loans.values()) if (!l.isReturned()) active.add(l);
        return active;
    }

    public long computeFine(String loanId) {
        Loan l = loans.get(loanId);
        if (l == null) return 0;
        return l.fine(LocalDate.now());
    }

    // ── Persistence
    public void save() throws IOException {
        if (persistence == null) return;
        persistence.save("books.dat", books);
        persistence.save("members.dat", members);
        persistence.save("loans.dat", loans);
    }

    @SuppressWarnings("unchecked")
    public void load() {
        if (persistence == null) return;
        try {
            Map<String, Book> b = persistence.load("books.dat");
            Map<String, Member> m = persistence.load("members.dat");
            Map<String, Loan> l = persistence.load("loans.dat");
            if (b != null) { books.clear(); books.putAll(b); }
            if (m != null) { members.clear(); members.putAll(m); }
            if (l != null) { loans.clear(); loans.putAll(l); }
        } catch (Exception ignored) {}
    }
}

// ───────────────────────────────────────────────
// File: LibraryApp.java (Main)
// ───────────────────────────────────────────────
import java.io.IOException;
import java.util.*;

public class LibraryApp {
    private static final Scanner sc = new Scanner(System.in);
    private static final Library lib = new Library();

    public static void main(String[] args) {
        System.out.println("=== Library Management System (Pure Java) ===");
        lib.attachPersistence(new Persistence("data"));
        lib.load();

        loop: while (true) {
            menu();
            String choice = input("Choose option: ");
            switch (choice) {
                case "1": addBook(); break;
                case "2": listBooks(); break;
                case "3": searchBooks(); break;
                case "4": removeBook(); break;
                case "5": addMember(); break;
                case "6": listMembers(); break;
                case "7": removeMember(); break;
                case "8": issueBook(); break;
                case "9": listLoans(false); break;
                case "10": listLoans(true); break;
                case "11": returnBook(); break;
                case "0":
                    try { lib.save(); } catch (IOException e) { System.out.println("Save failed: " + e.getMessage()); }
                    System.out.println("Bye!");
                    break loop;
                default:
                    System.out.println("Invalid");
            }
        }
    }

    private static void menu() {
        System.out.println();
        System.out.println("1) Add Book");
        System.out.println("2) List Books");
        System.out.println("3) Search Books");
        System.out.println("4) Remove Book");
        System.out.println("5) Add Member");
        System.out.println("6) List Members");
        System.out.println("7) Remove Member");
        System.out.println("8) Issue Book");
        System.out.println("9) List All Loans");
        System.out.println("10) List Active Loans");
        System.out.println("11) Return Book");
        System.out.println("0) Exit");
    }

    private static String input(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    // ── Book flows
    private static void addBook() {
        String title = input("Title: ");
        String author = input("Author: ");
        String isbn = input("ISBN (optional): ");
        int copies = readInt("Total copies: ", 1);
        Book b = lib.addBook(title, author, isbn.isEmpty()? null : isbn, copies);
        saveSilent();
        System.out.println("Added: " + b);
    }

    private static void listBooks() {
        System.out.println("— Books —");
        for (Book b : lib.listBooks()) System.out.println(b);
    }

    private static void searchBooks() {
        String k = input("Keyword: ");
        List<Book> found = lib.searchBooks(k);
        if (found.isEmpty()) System.out.println("No matches.");
        else found.forEach(System.out::println);
    }

    private static void removeBook() {
        String id = input("Book ID: ");
        boolean ok = lib.removeBook(id);
        if (ok) { saveSilent(); System.out.println("Removed."); }
        else System.out.println("Cannot remove (not found or active loans).");
    }

    // ── Member flows
    private static void addMember() {
        String name = input("Name: ");
        String email = input("Email: ");
        String phone = input("Phone: ");
        Member m = lib.addMember(name, email, phone);
        saveSilent();
        System.out.println("Added: " + m);
    }

    private static void listMembers() {
        System.out.println("— Members —");
        for (Member m : lib.listMembers()) System.out.println(m);
    }

    private static void removeMember() {
        String id = input("Member ID: ");
        boolean ok = lib.removeMember(id);
        if (ok) { saveSilent(); System.out.println("Removed."); }
        else System.out.println("Cannot remove (not found or has active loans).");
    }

    // ── Loan flows
    private static void issueBook() {
        String bookId = input("Book ID: ");
        String memberId = input("Member ID: ");
        lib.issueBook(bookId, memberId).ifPresentOrElse(l -> {
            saveSilent();
            System.out.println("Issued: " + l);
        }, () -> System.out.println("Issue failed (invalid IDs, no copies, or member limit reached)."));
    }

    private static void listLoans(boolean onlyActive) {
        System.out.println(onlyActive ? "— Active Loans —" : "— All Loans —");
        for (Loan l : lib.listLoans(onlyActive)) {
            long fine = l.fine(java.time.LocalDate.now());
            System.out.println(l + (fine > 0 ? (" | Fine (today): " + fine) : ""));
        }
    }

    private static void returnBook() {
        String loanId = input("Loan ID: ");
        lib.returnBook(loanId).ifPresentOrElse(fine -> {
            saveSilent();
            System.out.println("Returned. Fine due: " + fine);
        }, () -> System.out.println("Return failed (invalid ID or already returned)."));
    }

    // ── Helpers
    private static int readInt(String prompt, int min) {
        while (true) {
            String s = input(prompt);
            try {
                int v = Integer.parseInt(s);
                if (v >= min) return v;
            } catch (NumberFormatException ignored) {}
            System.out.println("Enter a valid number ≥ " + min);
        }
    }

    private static void saveSilent() {
        try { lib.save(); } catch (Exception ignored) {}
    }
}
