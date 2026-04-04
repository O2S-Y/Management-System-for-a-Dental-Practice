<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Statistiques"/>
<jsp:include page="/views/common/header.jsp"/>
<jsp:include page="/views/common/nav.jsp"/>
<div class="main-content">
    <div class="topbar">
        <h4><i class="bi bi-bar-chart-line me-2"></i>Tableau de bord — Statistiques</h4>
    </div>
    <jsp:include page="/views/common/flash.jsp"/>

    <%-- ─── KPI Cards ─────────────────────────────────────────────────── --%>
    <div class="row g-3 mb-4">
        <div class="col-md-3">
            <div class="card text-center p-3">
                <div style="font-size:2.5rem;font-weight:800;color:#028090;">${totalPatients}</div>
                <div class="text-muted">Patients enregistrés</div>
                <div style="height:4px;background:#028090;border-radius:4px;margin-top:.5rem;"></div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-center p-3">
                <div style="font-size:2.5rem;font-weight:800;color:#ef6c00;">${rdvToday}</div>
                <div class="text-muted">RDV aujourd'hui</div>
                <div style="height:4px;background:#ef6c00;border-radius:4px;margin-top:.5rem;"></div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-center p-3">
                <div style="font-size:2.5rem;font-weight:800;color:#2e7d32;">${consultsMois}</div>
                <div class="text-muted">Consultations (mois)</div>
                <div style="height:4px;background:#2e7d32;border-radius:4px;margin-top:.5rem;"></div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-center p-3">
                <div style="font-size:1.8rem;font-weight:800;color:#c2185b;">
                    <fmt:formatNumber value="${caMois}" maxFractionDigits="0"/> MAD
                </div>
                <div class="text-muted">CA ce mois (payé)</div>
                <div style="height:4px;background:#c2185b;border-radius:4px;margin-top:.5rem;"></div>
            </div>
        </div>
    </div>

    <div class="row g-3 mb-4">
        <%-- ─── CA par dentiste (CDC) ──────────────────────────────── --%>
        <div class="col-md-6">
            <div class="card h-100">
                <div class="card-header fw-bold" style="background:#0D3B4E;color:#fff;">
                    <i class="bi bi-person-badge me-1"></i>Chiffre d'affaires par dentiste
                </div>
                <div class="card-body p-0">
                    <c:choose>
                        <c:when test="${empty caParDentiste}">
                            <div class="text-muted text-center py-4">Aucune donnée</div>
                        </c:when>
                        <c:otherwise>
                            <div class="table-responsive">
                                <table class="table table-hover mb-0">
                                    <thead><tr><th>Dentiste</th><th class="text-end">CA (MAD)</th></tr></thead>
                                    <tbody>
                                        <c:forEach var="row" items="${caParDentiste}">
                                        <tr>
                                            <td class="fw-semibold">Dr. ${row[0]}</td>
                                            <td class="text-end fw-bold text-success">
                                                <fmt:formatNumber value="${row[1]}" maxFractionDigits="2"/>
                                            </td>
                                        </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <%-- ─── Taux RDV annulés (CDC) ────────────────────────────── --%>
        <div class="col-md-6">
            <div class="card h-100">
                <div class="card-header fw-bold" style="background:#0D3B4E;color:#fff;">
                    <i class="bi bi-calendar-x me-1"></i>Taux d'absences et annulations
                </div>
                <div class="card-body">
                    <div class="row g-3 text-center">
                        <div class="col-4">
                            <div style="font-size:2rem;font-weight:800;color:#334155;">${totalRdv}</div>
                            <div class="text-muted small">Total RDV</div>
                        </div>
                        <div class="col-4">
                            <div style="font-size:2rem;font-weight:800;color:#dc3545;">${nbAnnules}</div>
                            <div class="text-muted small">Annulés</div>
                        </div>
                        <div class="col-4">
                            <div style="font-size:2rem;font-weight:800;color:#f97316;">${nbNonHonore}</div>
                            <div class="text-muted small">Non honorés</div>
                        </div>
                    </div>
                    <hr>
                    <div class="mb-2">
                        <div class="d-flex justify-content-between small fw-semibold mb-1">
                            <span>% RDV annulés</span>
                            <span class="text-danger">${tauxAnnules}%</span>
                        </div>
                        <div class="progress" style="height:12px;">
                            <div class="progress-bar bg-danger" style="width:${tauxAnnules}%"></div>
                        </div>
                    </div>
                    <div>
                        <div class="d-flex justify-content-between small fw-semibold mb-1">
                            <span>% Non honorés</span>
                            <span class="text-warning">${tauxNonHonore}%</span>
                        </div>
                        <div class="progress" style="height:12px;">
                            <div class="progress-bar bg-warning" style="width:${tauxNonHonore}%"></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <%-- ─── Agenda du jour ────────────────────────────────────────────── --%>
    <div class="card">
        <div class="card-header" style="background:#0D3B4E;color:#fff;">
            <i class="bi bi-calendar3 me-1"></i>Rendez-vous du jour
        </div>
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${empty rdvList}">
                    <div class="text-center text-muted py-4">Aucun rendez-vous aujourd'hui</div>
                </c:when>
                <c:otherwise>
                    <div class="table-responsive">
                        <table class="table table-hover mb-0">
                            <thead><tr><th>Heure</th><th>Patient</th><th>Dentiste</th><th>Motif</th><th>Statut</th></tr></thead>
                            <tbody>
                                <c:forEach var="rv" items="${rdvList}">
                                <tr>
                                    <td class="fw-bold">${rv.dateHeure.toLocalTime()}</td>
                                    <td>${rv.nomCompletPatient}</td>
                                    <td>${rv.nomCompletDentiste}</td>
                                    <td><span class="badge bg-secondary">${rv.motif.libelle}</span></td>
                                    <td><span class="badge bg-${rv.statut.badgeColor}">${rv.statut.libelle}</span></td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>
<jsp:include page="/views/common/footer.jsp"/>
