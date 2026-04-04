package ma.fst.sgcd.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import ma.fst.sgcd.model.RendezVous;
import ma.fst.sgcd.model.Utilisateur;
import ma.fst.sgcd.model.enums.MotifRDV;
import ma.fst.sgcd.model.enums.Role;
import ma.fst.sgcd.repository.*;
import ma.fst.sgcd.service.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GET  /rdv                 → agenda du jour
 * GET  /rdv?action=add      → formulaire ajout
 * GET  /rdv?date=YYYY-MM-DD → agenda par date
 * GET  /rdv?q=texte         → recherche
 * POST /rdv?action=save     → planifier
 * POST /rdv?action=statut   → changer statut
 * POST /rdv?action=delete   → supprimer
 */
@WebServlet("/rdv")
public class RendezVousServlet extends HttpServlet {

    // Horaires cabinet selon CDC : 9h–12h et 14h–19h
    private static final LocalTime MATIN_DEBUT  = LocalTime.of( 9, 0);
    private static final LocalTime MATIN_FIN    = LocalTime.of(12, 0);
    private static final LocalTime APREM_DEBUT  = LocalTime.of(14, 0);
    private static final LocalTime APREM_FIN    = LocalTime.of(19, 0);

    private RendezVousService rdvService;
    private PatientService    patientService;

    @Override
    public void init() {
        rdvService     = new RendezVousService(new RendezVousRepository());
        patientService = new PatientService(new PatientRepository(), new DossierMedicalRepository());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("add".equals(action)) { showAddForm(req, resp); return; }
        showList(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        switch (action == null ? "" : action) {
            case "save"   -> saveRdv(req, resp);
            case "statut" -> changeStatut(req, resp);
            case "delete" -> deleteRdv(req, resp);
            default       -> resp.sendRedirect(req.getContextPath() + "/rdv");
        }
    }

    // ─── Agenda avec recherche ────────────────────────────────────────────
    private void showList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String q         = req.getParameter("q");
        String dateParam = req.getParameter("date");
        LocalDate date   = (dateParam != null && !dateParam.isBlank())
                ? LocalDate.parse(dateParam) : LocalDate.now();

        List<RendezVous> rdvList = rdvService.findByDate(date);

        if (q != null && !q.isBlank()) {
            String qLow = q.toLowerCase();
            rdvList = rdvList.stream().filter(rv ->
                (rv.getNomPatient()    != null && rv.getNomPatient().toLowerCase().contains(qLow)) ||
                (rv.getPrenomPatient() != null && rv.getPrenomPatient().toLowerCase().contains(qLow)) ||
                (rv.getNomDentiste()   != null && rv.getNomDentiste().toLowerCase().contains(qLow)) ||
                rv.getMotif().getLibelle().toLowerCase().contains(qLow) ||
                rv.getStatut().getLibelle().toLowerCase().contains(qLow)
            ).collect(Collectors.toList());
        }

        req.setAttribute("rdvList",      rdvList);
        req.setAttribute("dateSelected", date);
        req.setAttribute("searchQuery",  q);
        req.getRequestDispatcher("/views/rdv/list.jsp").forward(req, resp);
    }

    // ─── Formulaire ajout ────────────────────────────────────────────────
    private void showAddForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("patients", patientService.findAll());
        req.setAttribute("dentistes", new UtilisateurRepository().findByRole(Role.DENTISTE));
        req.setAttribute("motifs", MotifRDV.values());
        req.getRequestDispatcher("/views/rdv/add.jsp").forward(req, resp);
    }

    // ─── Planifier un RDV ────────────────────────────────────────────────
    private void saveRdv(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        LocalDate date;
        LocalTime heure;
        try {
            date  = LocalDate.parse(req.getParameter("date"));
            heure = LocalTime.parse(req.getParameter("heure"));
        } catch (Exception e) {
            req.getSession().setAttribute("flash_error",
                "Date ou heure invalide.");
            resp.sendRedirect(req.getContextPath() + "/rdv?action=add");
            return;
        }

        // ── Validation horaires cabinet (CDC : 9h–12h et 14h–19h) ────────
        boolean heureValide =
            (!heure.isBefore(MATIN_DEBUT) && heure.isBefore(MATIN_FIN)) ||
            (!heure.isBefore(APREM_DEBUT) && heure.isBefore(APREM_FIN));

        if (!heureValide) {
            req.setAttribute("errorHoraire",
                "Horaire invalide. Le cabinet est ouvert de 9h à 12h et de 14h à 19h.");
            req.setAttribute("patients",  patientService.findAll());
            req.setAttribute("dentistes", new UtilisateurRepository().findByRole(Role.DENTISTE));
            req.setAttribute("motifs",    MotifRDV.values());
            req.getRequestDispatcher("/views/rdv/add.jsp").forward(req, resp);
            return;
        }

        RendezVous rv = new RendezVous();
        rv.setIdPatient(Long.parseLong(req.getParameter("idPatient")));
        rv.setIdDentiste(Long.parseLong(req.getParameter("idDentiste")));
        rv.setMotif(MotifRDV.valueOf(req.getParameter("motif")));
        rv.setNotes(req.getParameter("notes"));
        rv.setDuree(Integer.parseInt(req.getParameter("duree")));
        rv.setDateHeure(LocalDateTime.of(date, heure));

        Utilisateur u = (Utilisateur) req.getSession().getAttribute("utilisateur");
        if (u != null && u.getRole() == Role.ASSISTANTE)
            rv.setIdAssistante(u.getIdUtilisateur());

        rdvService.planifier(rv);
        req.getSession().setAttribute("flash_success", "Rendez-vous planifié avec succès.");
        resp.sendRedirect(req.getContextPath() + "/rdv");
    }

    // ─── Changer le statut ────────────────────────────────────────────────
    private void changeStatut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Long   id   = Long.parseLong(req.getParameter("id"));
        String type = req.getParameter("type");
        switch (type) {
            case "arrivee" -> rdvService.marquerArrivee(id);
            case "encours" -> rdvService.marquerEnCours(id);
            case "termine" -> rdvService.marquerTermine(id);
            case "annuler" -> rdvService.annuler(id);
        }
        String referer = req.getHeader("Referer");
        resp.sendRedirect(referer != null ? referer : req.getContextPath() + "/rdv");
    }

    // ─── Supprimer ────────────────────────────────────────────────────────
    private void deleteRdv(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        new RendezVousRepository().delete(Long.parseLong(req.getParameter("id")));
        req.getSession().setAttribute("flash_success", "Rendez-vous supprimé.");
        resp.sendRedirect(req.getContextPath() + "/rdv");
    }
}
