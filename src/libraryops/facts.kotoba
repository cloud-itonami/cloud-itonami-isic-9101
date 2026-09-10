(ns libraryops.facts
  "Per-jurisdiction legal-deposit/non-circulating-collection AND
  conservation-professional-standards regulatory catalog -- the
  G2-style spec-basis table the Library Governor checks every
  `:jurisdiction/assess` proposal against ('did the advisor cite an
  OFFICIAL public source for this jurisdiction's requirements, or did
  it invent one?').

  This blueprint's own text (docs/business-model.md's own Trust
  Controls: 'lending outside policy is blocked; preservation is
  auditable') names two real, distinct regulatory concerns: the
  general legal-deposit/non-circulating-collection framework
  restricting which materials may ever leave the premises
  (independent of a conservator's own qualification), and a SEPARATE
  professional-conservation-ethics regime specifically requiring a
  qualified conservator's sign-off before treating rare/fragile/
  special-collection materials (independent of whether the material
  is legally restricted from circulation -- a fully-circulating item
  can still require conservator sign-off for repair, and a legal-
  deposit item can be a routine preservation case needing no special
  conservator involvement). Each jurisdiction entry below therefore
  cites BOTH the general legal-deposit/non-circulating law AND a
  SEPARATE professional-conservation-standards regime.

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries. Like
  `adminops`/8411's own appeal-rights sub-citation, ALL FOUR seeded
  jurisdictions actually have a real conservation-standards
  sub-citation here, reported honestly (a full-coverage sub-citation,
  matching `quarryops`/0810's own blast-safety and `agronomyops`/
  0162's own water-buffer full coverage rather than
  `hospitalityops`/5510's own honest single-jurisdiction gap).")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  intake/cataloging/lending-record evidence set (PLUS a conservator-
  sign-off record for every seeded jurisdiction); `:legal-basis` /
  `:owner-authority` / `:provenance` are the G2 citation the governor
  requires before any `:jurisdiction/assess` proposal can commit.
  `:conservation-owner-authority` / `:conservation-legal-basis` /
  `:conservation-provenance` are the SEPARATE professional-
  conservation citation the governor's `conservator-sign-off-
  missing?` check is grounded in."
  {"JPN" {:name "Japan"
          :owner-authority "国立国会図書館 (National Diet Library, NDL)"
          :legal-basis "国立国会図書館法 (National Diet Library Act) 第24条 (納本制度)"
          :national-spec "納本制度による非貸出資料の取扱い基準"
          :provenance "https://www.ndl.go.jp/jp/collect/deposit/index.html"
          :required-evidence ["受入記録 (intake record)"
                              "目録記録 (cataloging record)"
                              "貸出記録 (lending record)"
                              "保存処置承認記録 (conservator-sign-off record)"]
          :conservation-owner-authority "文化庁 (Agency for Cultural Affairs)"
          :conservation-legal-basis "文化財保護法 (Act on Protection of Cultural Properties)"
          :conservation-provenance "https://www.bunka.go.jp/seisaku/bunkazai/"}
   "USA" {:name "United States"
          :owner-authority "Library of Congress (Copyright Office)"
          :legal-basis "Copyright Act, 17 U.S.C. §407 (mandatory deposit)"
          :national-spec "Copyright Office mandatory-deposit and non-circulating collection rules"
          :provenance "https://www.copyright.gov/title17/92chap4.html"
          :required-evidence ["Intake record"
                              "Cataloging record"
                              "Lending record"
                              "Conservator-sign-off record"]
          :conservation-owner-authority "American Institute for Conservation (AIC)"
          :conservation-legal-basis "AIC Code of Ethics and Guidelines for Practice"
          :conservation-provenance "https://www.culturalheritage.org/about-conservation/code-of-ethics"}
   "GBR" {:name "United Kingdom"
          :owner-authority "The British Library / Legal Deposit Libraries"
          :legal-basis "Legal Deposit Libraries Act 2003"
          :national-spec "Legal deposit non-circulating collection rules"
          :provenance "https://www.legislation.gov.uk/ukpga/2003/28/contents"
          :required-evidence ["Intake record"
                              "Cataloging record"
                              "Lending record"
                              "Conservator-sign-off record"]
          :conservation-owner-authority "Institute of Conservation (Icon)"
          :conservation-legal-basis "Icon Professional Standards and Code of Conduct"
          :conservation-provenance "https://icon.org.uk/about-us/professional-standards"}
   "DEU" {:name "Germany"
          :owner-authority "Deutsche Nationalbibliothek (DNB)"
          :legal-basis "Gesetz über die Deutsche Nationalbibliothek (DNBG) §16 (Pflichtablieferung)"
          :national-spec "DNBG non-circulating deposit-collection provisions"
          :provenance "https://www.gesetze-im-internet.de/dnbg/__16.html"
          :required-evidence ["Aufnahmeprotokoll (intake record)"
                              "Katalogisierungsprotokoll (cataloging record)"
                              "Ausleiheprotokoll (lending record)"
                              "Restaurierungsfreigabenachweis (conservator-sign-off record)"]
          :conservation-owner-authority "Verband der Restauratoren (VDR)"
          :conservation-legal-basis "VDR Berufsethische Richtlinien (professional code of ethics)"
          :conservation-provenance "https://www.restauratoren.de/"}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to lend an item
  or preserve it on that basis."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-9101 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `libraryops.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))

(defn conservation-spec-basis
  "The jurisdiction's professional-conservation-standards requirement
  map, or nil -- nil means this jurisdiction has NO formal
  conservation-professional-standards regime this catalog is aware
  of. In this R0 catalog all four seeded jurisdictions actually have
  one, reported honestly (a full-coverage sub-citation, matching
  `quarryops`/0810's own blast-safety and `agronomyops`/0162's own
  water-buffer full coverage)."
  [iso3]
  (when-let [sb (spec-basis iso3)]
    (when (:conservation-owner-authority sb)
      (select-keys sb [:conservation-owner-authority :conservation-legal-basis :conservation-provenance]))))
