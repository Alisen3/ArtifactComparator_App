// src/pages/UploadArtifacts.jsx
import React, { useEffect, useMemo, useState } from "react";
import { useAuth, api, API_BASE_URL } from "../context/AuthContext";
import {
    Upload, PlusCircle, Search, Tag, FolderPlus, SlidersHorizontal,
    FileText, Trash2, CheckCircle2, X, ChevronRight, Layers,
    RefreshCw, ExternalLink
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

/* --------- THEME --------- */
const T = {
    bg: "#0f1115",
    panel: "#151821",
    panelSoft: "#202532",
    stroke: "#2b303b",
    text: "#E8EAF2",
    muted: "#AEB6C8",
    brand: "#7A3BE8",
    brand2: "#22C55E",
    pillBg: "#1c2230",
};

const pageWrap = {
    minHeight: "100vh",
    background: T.bg,
};

const container = {
    maxWidth: 1400,
    margin: "0 auto",
    padding: "32px 24px",
};

// "justifyContent" olarak düzeltildi
const hStack = (gap = 8, justify = "flex-start", align = "center") => ({
    display: "flex",
    gap,
    justifyContent: justify,
    alignItems: align,
});

const vStack = (gap = 8) => ({ display: "flex", flexDirection: "column", gap });

const card = (withHeader = false) => ({
    border: `1px solid ${T.stroke}`,
    borderRadius: 16,
    background: T.panel,
    overflow: "hidden",
});

const cardHeader = {
    ...hStack(12, "space-between"),
    padding: "12px 16px",
    borderBottom: `1px solid ${T.stroke}`,
};

const cardBody = { padding: 16 };

const inputBase = {
    background: "transparent",
    outline: "none",
    border: "none",
    color: T.text,
    fontSize: 14,
};

const pill = {
    fontSize: 12,
    borderRadius: 12,
    padding: "4px 8px",
    background: T.pillBg,
    color: T.muted,
};

const btnBase = {
    display: "inline-flex",
    alignItems: "center",
    gap: 8,
    borderRadius: 16,
    padding: "8px 14px",
    fontSize: 14,
    cursor: "pointer",
    userSelect: "none",
    border: "1px solid transparent",
};

const Button = ({ children, icon: Icon, variant = "solid", style, ...rest }) => {
    const s = useMemo(() => {
        if (variant === "solid")
            return { ...btnBase, background: T.brand, color: "#fff" };
        if (variant === "subtle")
            return { ...btnBase, background: T.panelSoft, color: T.text, borderColor: T.stroke };
        if (variant === "ghost")
            return { ...btnBase, background: "transparent", color: T.text, borderColor: T.stroke };
        return btnBase;
    }, [variant]);
    return (
        <button {...rest} style={{ ...s, ...style }}>
            {Icon && <Icon size={16} />}
            {children}
        </button>
    );
};

/* --------- TABLE --------- */
const tableHeader = {
    display: "grid",
    gridTemplateColumns: "6fr 3fr 3fr",
    padding: "10px 16px",
    fontSize: 12,
    color: T.muted,
    borderBottom: `1px solid ${T.stroke}`,
};

const rowBase = {
    display: "grid",
    gridTemplateColumns: "6fr 3fr 3fr",
    alignItems: "center",
    padding: "12px 16px",
    borderBottom: `1px solid ${T.stroke}`,
};

function ArtifactRow({ item, onClickVersions, onDelete }) {
    return (
        <div style={rowBase}>
            <div style={hStack(12, "flex-start", "center")}>
                <FileText size={18} color={T.muted} />
                <div style={vStack(4)}>
                    <div style={{ color: T.text, fontSize: 14 }}>{item.filename}</div>
                    <div style={{ color: T.muted, fontSize: 12 }}>
                        v{item.version} • {item.sizeLabel} • {item.mimeShort}
                    </div>
                </div>
            </div>
            <div style={{ color: T.muted, fontSize: 13 }}>
                {item.tags?.length ? item.tags.join(", ") : "—"}
            </div>
            <div style={{ ...hStack(12, "flex-end", "center") }}>
                <Button variant="subtle" icon={Layers} onClick={() => onClickVersions(item)}>Versions</Button>
                {/* 'onDelete' artık backend'i çağıracak */}
                <Button variant="ghost" icon={Trash2} onClick={() => onDelete(item)}>Delete</Button>
            </div>
        </div>
    );
}

/* --------- DRAWER --------- */
function VersionsDrawer({ open, onClose, file, onMakeCurrent }) {
    return (
        <AnimatePresence>
            {open && (
                <motion.div
                    initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                    style={{ position: "fixed", inset: 0, zIndex: 50, display: "flex" }}
                >
                    <div onClick={onClose} style={{ flex: 1 }} />
                    <motion.aside
                        initial={{ x: 400 }} animate={{ x: 0 }} exit={{ x: 400 }}
                        style={{
                            width: 380, maxWidth: "88vw", height: "100%", borderLeft: `1px solid ${T.stroke}`,
                            background: T.panel, display: "flex", flexDirection: "column",
                        }}
                    >
                        <div style={cardHeader}>
                            <div style={{ color: T.text, fontWeight: 600, fontSize: 14 }}>
                                Versions – {file?.filename}
                            </div>
                            <button onClick={onClose} style={{ background: "transparent", border: "none" }}>
                                <X color={T.muted} />
                            </button>
                        </div>
                        <div style={{ padding: 12, overflowY: "auto" }}>
                            {(file?.versions || []).map(v => (
                                <div key={v.id}
                                     style={{
                                         border: `1px solid ${T.stroke}`, borderRadius: 12, background: T.panelSoft,
                                         padding: 12, marginBottom: 10,
                                     }}>
                                    <div style={hStack(12, "space-between")}>
                                        <div>
                                            <div style={{ color: T.text, fontSize: 14 }}>v{v.number}</div>
                                            <div style={{ color: T.muted, fontSize: 12 }}>Uploaded {v.uploadedAt}</div>
                                        </div>
                                        <div style={hStack(8)}>
                                            {/* storageUrl artık backend'den (hem listeleme hem yükleme) geliyor */}
                                            <Button 
                                                variant="ghost" 
                                                icon={ExternalLink}
                                                onClick={() => window.open(`${API_BASE_URL}${v.storageUrl}`, '_blank', 'noopener,noreferrer')}
                                            >
                                                Open
                                            </Button>
                                            <Button variant="ghost" icon={RefreshCw} onClick={() => onMakeCurrent(file, v)}>
                                                Make current
                                            </Button>
                                        </div>
                                    </div>
                                </div>
                            ))}
                            {(!file?.versions || file?.versions.length === 0) && (
                                <div style={{ color: T.muted, fontSize: 14, padding: 8 }}>No versions yet.</div>
                            )}
                        </div>
                    </motion.aside>
                </motion.div>
            )}
        </AnimatePresence>
    );
}

/* --------- UPLOAD WIZARD (DÜZELTİLDİ) --------- */
function UploadWizard({ open, onClose, onUploaded }) {
    const [file, setFile] = useState(null);
    const [step, setStep] = useState(1);
    const [simulateDup, setSimulateDup] = useState(false);
    const [msg, setMsg] = useState(null);

    const reset = () => { setFile(null); setStep(1); setSimulateDup(false); setMsg(null); };

    const doUpload = async () => {
        if (!file) { setMsg({ t: "error", m: "Please choose a file." }); return; }
        try {
            setMsg({ t: "info", m: "Uploading…" });
            const localMimeType = file.type || "application/octet-stream";
            const fd = new FormData();
            fd.append("file", file);

            // 'ownerId' gönderilmiyor, token'dan alınıyor
            // DÜZELTME: Backend (Java) artık tam entity döndürüyor
            const { data } = await api.post("/api/store-artifacts/upload", fd, {
                headers: { "Content-Type": "multipart/form-data" },
            });
            // 'data' artık {id, filename, sizeBytes, versionNumber, createdAt, storageUrl, ...} içeriyor

            setMsg({ t: "success", m: `Uploaded: ${data.filename}` });

            // --- DÜZELTME BURADA ---
            // 'onUploaded' fonksiyonunu, backend'den gelen
            // tam 'data' nesnesini kullanacak şekilde güncelliyoruz.
            onUploaded({
                id: data.id,
                filename: data.filename,
                sizeLabel: toSize(data.sizeBytes), // Düzeltildi: data.size -> data.sizeBytes
                mimeShort: (localMimeType).split("/").pop()?.toUpperCase() || "FILE",
                version: data.versionNumber, // Düzeltildi: 1 -> data.versionNumber
                tags: [],
                versions: [{
                    id: data.id,
                    number: data.versionNumber, // Düzeltildi: 1 -> data.versionNumber
                    uploadedAt: new Date(data.createdAt).toISOString().slice(0, 10), // Düzeltildi: new Date() -> data.createdAt
                    storageUrl: data.storageUrl, // DÜZELTME: "#" -> data.storageUrl
                }]
            });
            setStep(3);
        } catch (err) {
            const isDup = err?.response?.status === 409;
            setMsg({ t: "error", m: isDup ? "Duplicate file for this owner (same content hash)." : (err?.response?.data?.error || err.message) });
        }
    };

    // --- UploadWizard JSX (Değişiklik yok) ---
    return (
        <AnimatePresence>
            {open && (
                <motion.div
                    initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                    style={{
                        position: "fixed", inset: 0, zIndex: 60, display: "flex",
                        alignItems: "center", justifyContent: "center", background: "rgba(0,0,0,0.6)"
                    }}
                >
                    <motion.div
                        initial={{ y: 30, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 20, opacity: 0 }}
                        style={{ width: 980, maxWidth: "95vw", borderRadius: 24, overflow: "hidden", ...card() }}
                    >
                        <div style={cardHeader}>
                            <div style={{ color: T.text, fontWeight: 600 }}>Upload Artifact</div>
                            <button onClick={() => { reset(); onClose(); }} style={{ background: "transparent", border: "none" }}>
                                <X color={T.muted} />
                            </button>
                        </div>

                        {/* Adım 1, 2, 3 JSX'leri (Değişiklik yok) */}
                        <div style={{ padding: 24, display: "grid", gridTemplateRows: "auto", gap: 16 }}>
                            {step === 1 && (
                                <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 24 }}>
                                    <div style={vStack(8)}>
                                        <div style={{ color: T.text, fontSize: 14 }}>Choose file</div>
                                        <label style={{ height: 160, borderRadius: 16, border: `1px solid ${T.stroke}`, background: T.panelSoft, ...hStack(8, "center", "center"), flexDirection: "column", cursor: "pointer" }}>
                                            <Upload />
                                            <div style={{ color: T.muted, fontSize: 14 }}>Drag & drop or click to select</div>
                                            <input type="file" style={{ display: "none" }} onChange={(e) => setFile(e.target.files?.[0] || null)} />
                                        </label>
                                        <div style={{ color: T.muted, fontSize: 12 }}>{file ? `Selected: ${file.name} (${toSize(file.size)})` : "Ready to upload."}</div>
                                    </div>
                                    <div style={vStack(8)}>
                                        <div style={{ color: T.text, fontSize: 14 }}>Options</div>
                                        <button onClick={() => setSimulateDup(s => !s)} style={{ ...btnBase, width: "100%", justifyContent: "center", background: T.panelSoft, color: T.text, borderColor: T.stroke }}>
                                            {simulateDup ? "Duplicate ON" : "Simulate duplicate"}
                                        </button>
                                        <div style={{ ...hStack(8), flexWrap: "wrap" }}><span style={pill}>PDF</span><span style={pill}>Max 25MB</span><span style={pill}>Previewable</span></div>
                                    </div>
                                    <div style={{ gridColumn: "1 / span 2", ...hStack(8, "flex-end") }}><Button icon={ChevronRight} onClick={() => setStep(2)}>Next</Button></div>
                                </div>
                            )}
                            {step === 2 && (
                                <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 24 }}>
                                    <div style={vStack(8)}>
                                        <div style={{ color: T.text, fontSize: 14 }}>Tag & Categorize</div>
                                        <div style={hStack(8)}><Button variant="subtle" icon={Tag}>Add tag</Button><Button variant="subtle" icon={FolderPlus}>Choose folder</Button></div>
                                        <input placeholder="No tags yet" style={{ ...inputBase, width: "100%", border: `1px solid ${T.stroke}`, background: T.panelSoft, borderRadius: 12, padding: "10px 12px" }} />
                                        <div style={{ color: T.muted, fontSize: 12 }}>You can add tags or choose a folder.</div>
                                    </div>
                                    <div>
                                        <div style={{ color: T.text, fontSize: 14, marginBottom: 8 }}>Preview</div>
                                        <div style={{ height: 140, borderRadius: 12, border: `1px solid ${T.stroke}`, background: T.panelSoft, ...hStack(8, "center", "center"), color: T.muted, fontSize: 13 }}>(auto-generated thumbnail)</div>
                                    </div>
                                    {msg && (<div style={{ gridColumn: "1 / span 2", color: msg.t === "error" ? "#f87171" : msg.t === "success" ? T.brand2 : T.muted, fontSize: 14 }}>{msg.m}</div>)}
                                    <div style={{ gridColumn: "1 / span 2", ...hStack(8, "space-between") }}><Button variant="ghost" onClick={() => setStep(1)}>Back</Button><Button icon={PlusCircle} onClick={doUpload}>Upload</Button></div>
                                </div>
                            )}
                            {step === 3 && (
                                <div style={{ border: `1px solid ${T.stroke}`, borderRadius: 12, background: T.panelSoft, padding: 12, ...hStack(12, "space-between") }}>
                                    <div style={hStack(12, "flex-start", "center")}><CheckCircle2 color={T.brand2} /><div><div style={{ color: T.text, fontSize: 14 }}>Stored & Indexed</div><div style={{ color: T.muted, fontSize: 12 }}>You can close this dialog now.</div></div></div>
                                    <Button onClick={() => { reset(); onClose(); }}>Finish</Button>
                                </div>
                            )}
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
}

/* --------- PAGE (Yükleme, Listeleme ve Silme Fonksiyonları) --------- */
export default function UploadArtifacts() {
    const [showWizard, setShowWizard] = useState(false);
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [drawerItem, setDrawerItem] = useState(null);
    const [rows, setRows] = useState([]);
    const [isLoading, setIsLoading] = useState(true); // Yüklenme durumu eklendi

    // --- LİSTELEME FONKSİYONU ---
    // Sayfa açıldığında çalışır ve 'my-artifacts' endpoint'inden veri çeker
    useEffect(() => {
        const fetchArtifacts = async () => {
            setIsLoading(true);
            try {
                // Java'ya eklediğimiz yeni GET endpoint'ini çağırıyoruz
                const { data } = await api.get("/api/store-artifacts/my-artifacts");

                // Backend'den gelen veriyi React UI'ın beklediği formata çeviriyoruz
                const formattedRows = data.map(item => ({
                    id: item.id,
                    filename: item.filename,
                    sizeLabel: toSize(item.sizeBytes),
                    mimeShort: (item.mimeType || "file").split("/").pop()?.toUpperCase(),
                    version: item.versionNumber,
                    tags: [], // (Tag özelliği henüz backend'de yok, o yüzden boş dizi)
                    versions: [{
                        id: item.id,
                        number: item.versionNumber,
                        uploadedAt: new Date(item.createdAt).toISOString().slice(0, 10),
                        storageUrl: item.storageUrl, // storageUrl'i backend'den alıyoruz
                    }]
                }));

                setRows(formattedRows); // State'i API verisiyle doldur

            } catch (err) {
                console.error("Artifact'ler yüklenemedi:", err);
                setRows([]); // Hata olursa listeyi boşalt
            }
            setIsLoading(false);
        };

        fetchArtifacts();
    }, []); // Boş dependency array '[]' sayesinde bu sadece sayfa ilk açıldığında çalışır

    // --- YÜKLEME FONKSİYONU ---
    // Yeni dosya yükleyince listeyi günceller (sayfayı yenilemeden)
    const onUploaded = (a) => setRows(prev => [a, ...prev]);

    const openVersions = (item) => { setDrawerItem(item); setDrawerOpen(true); };
    const makeCurrent = (file, version) => setRows(prev => prev.map(r => r.id === file.id ? { ...r, version: version.number } : r));

    // --- SİLME FONKSİYONU (DÜZELTİLDİ) ---
    const doDelete = async (item) => {
        // Önce kullanıcıya bir onay sorusu soruyoruz
        if (!window.confirm(`"${item.filename}" dosyasını kalıcı olarak silmek istediğinizden emin misiniz?`)) {
            return; // Kullanıcı "İptal" derse işlemi durdur
        }

        try {
            // Adım 1: Backend'e SİLME isteği gönder (Java'da oluşturduğumuz endpoint'e)
            // api nesnesi (AuthContext'ten) token'ı otomatik olarak ekleyecektir
            await api.delete(`/api/store-artifacts/${item.id}`);

            // Adım 2: Sadece API isteği başarılı olursa (hata fırlatmazsa),
            // o zaman ekrandan (React state'inden) kaldır.
            setRows(prev => prev.filter(r => r.id !== item.id));

        } catch (err) {
            console.error("Silme işlemi başarısız:", err);
            // Kullanıcıya bir hata mesajı göster
            alert("Dosya silinemedi: " + (err?.response?.data?.error || err.message));
        }
    };
    // --- --- ---

    // --- JSX (RENDER KISMI) ---
    return (
        <div style={pageWrap}>
            <div style={container}>
                {/* Top bar */}
                <div style={{ ...hStack(12, "space-between") }}>
                    <div style={{ fontSize: 28, fontWeight: 600, color: T.text }}>Dashboard</div>
                    <div style={hStack(12)}>
                        {/* search */}
                        <div style={{
                            ...hStack(8), border: `1px solid ${T.stroke}`, borderRadius: 16,
                            background: T.panel, padding: "8px 12px"
                        }}>
                            <Search size={16} color={T.muted} />
                            <input placeholder="Search documents…" style={{ ...inputBase, width: 240 }} />
                        </div>
                        <Button icon={PlusCircle} onClick={() => setShowWizard(true)}>New upload</Button>
                    </div>
                </div>

                {/* content: 8/4 kolon */}
                <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 24, marginTop: 24 }}>
                    {/* left panel */}
                    <div>
                        <div style={card(true)}>
                            <div style={cardHeader}>
                                <div>
                                    <div style={{ color: T.text, fontSize: 14 }}>All artifacts</div>
                                    <div style={{ color: T.muted, fontSize: 12 }}>Upload, version and manage documents</div>
                                </div>
                            </div>
                            <div style={cardBody}>
                                <div style={tableHeader}>
                                    <div>Name</div>
                                    <div>Tags</div>
                                    <div style={{ textAlign: "right" }}>Actions</div>
                                </div>

                                {/* rows (Artık API'den gelen veriyi gösteriyor) */}
                                {isLoading ? (
                                    <div style={{ padding: "16px", color: T.muted, fontSize: 14 }}>
                                        Artifact'ler yükleniyor...
                                    </div>
                                ) : rows.length === 0 ? (
                                    <div style={{ padding: "16px", color: T.muted, fontSize: 14 }}>
                                        No uploads yet. Click “New upload”.
                                    </div>
                                ) : rows.map(r => (
                                    <ArtifactRow key={r.id} item={r} onClickVersions={openVersions} onDelete={doDelete} />
                                ))}
                            </div>
                        </div>
                    </div>

                    {/* right panel (Değişiklik yok) */}
                    <div style={vStack(24)}>
                        <div style={card(true)}>
                            <div style={cardHeader}>
                                <div style={{ color: T.text, fontSize: 14 }}>Quick actions</div>
                                <Button variant="ghost" icon={SlidersHorizontal}>More</Button>
                            </div>
                            <div style={cardBody}>
                                <div style={{ ...hStack(8), flexWrap: "wrap" }}>
                                    <Button variant="subtle" icon={Upload} onClick={() => setShowWizard(true)}>Upload file</Button>
                                    <Button variant="subtle" icon={Tag}>Tag</Button>
                                    <Button variant="subtle" icon={FolderPlus}>New folder</Button>
                                </div>
                            </div>
                        </div>

                        <div style={card(true)}>
                            <div style={cardHeader}>
                                <div>
                                    <div style={{ color: T.text, fontSize: 14 }}>Filters</div>
                                    <div style={{ color: T.muted, fontSize: 12 }}>Narrow down your list</div>
                                </div>
                            </div>
                            <div style={cardBody}>
                                <div style={{ ...hStack(8), flexWrap: "wrap" }}>
                                    <span style={pill}>Type: PDF</span>
                                    {/* DÜZELTME: 'style{...pill}' -> 'style={{...pill}}' */}
                                    <span style={{...pill}}>Updated This week</span>
                                    <span style={pill}>Has preview</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <UploadWizard open={showWizard} onClose={() => setShowWizard(false)} onUploaded={onUploaded} />
            <VersionsDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} file={drawerItem} onMakeCurrent={makeCurrent} />
        </div>
    );
}


/* --------- helpers --------- */
function toSize(n) {
    if (n == null) return "—";
    const KB = 1024, MB = KB * 1024;
    if (n >= MB) return (n / MB).toFixed(1) + "MB";
    if (n >= KB) return Math.round(n / KB) + "KB";
    return n + "B";
}

// DİKKAT: 'demoRow' ve 'cryptoRandom' artık
// 'useEffect' API'den veri çektiği için KULLANILMIYOR.
// Ancak referans olarak kalmalarında bir sakınca yok.
function demoRow(name, mimeShort, sizeLabel, version, tags) {
    const id = Math.round(Math.random() * 100000);
    const today = new Date().toISOString().slice(0, 10);
    return {
        id, filename: name, sizeLabel, mimeShort, version, tags,
        versions: Array.from({ length: version }).map((_, i) => ({
            id: id + (i + 1),
            number: i + 1,
            uploadedAt: plusDays(today, -(version - (i + 1))),
            storageUrl: "#",
        })),
    };
}
function cryptoRandom() {
    return "id-" + Math.random().toString(36).slice(2);
}
function plusDays(iso, d) {
    const t = new Date(iso); t.setDate(t.getDate() + d); return t.toISOString().slice(0, 10);
}
