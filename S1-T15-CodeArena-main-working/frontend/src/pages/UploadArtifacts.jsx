// src/pages/UploadArtifacts.jsx
import React, { useEffect, useMemo, useState } from "react";
import { useAuth, api } from "../context/AuthContext";
import {
    Upload, PlusCircle, Search, Tag, FolderPlus, SlidersHorizontal,
    FileText, Trash2, CheckCircle2, X, ChevronRight, Layers,
    RefreshCw, ExternalLink, Clock, UploadCloud, FileJson, Edit
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

const pageWrap = { minHeight: "100vh", background: T.bg };
const container = { maxWidth: 1400, margin: "0 auto", padding: "32px 24px" };

const hStack = (gap = 8, justify = "flex-start", align = "center") => ({ display: "flex", gap, justifyContent: justify, alignItems: align });
const vStack = (gap = 8) => ({ display: "flex", flexDirection: "column", gap });
const card = () => ({ border: `1px solid ${T.stroke}`, borderRadius: 16, background: T.panel, overflow: "hidden" });
const cardHeader = { ...hStack(12, "space-between"), padding: "12px 16px", borderBottom: `1px solid ${T.stroke}` };
const cardBody = { padding: 16 };
const inputBase = { background: "transparent", outline: "none", border: "none", color: T.text, fontSize: 14 };
const pill = { fontSize: 12, borderRadius: 12, padding: "4px 8px", background: T.pillBg, color: T.muted };

const btnBase = { display: "inline-flex", alignItems: "center", gap: 8, borderRadius: 16, padding: "8px 14px", fontSize: 14, cursor: "pointer", userSelect: "none", border: "1px solid transparent" };

const Button = ({ children, icon: Icon, variant = "solid", style, type = "button", ...rest }) => {
    const s = useMemo(() => {
        if (variant === "solid") return { ...btnBase, background: T.brand, color: "#fff" };
        if (variant === "subtle") return { ...btnBase, background: T.panelSoft, color: T.text, borderColor: T.stroke };
        if (variant === "ghost") return { ...btnBase, background: "transparent", color: T.text, borderColor: T.stroke };
        return btnBase;
    }, [variant]);
    return <button type={type} {...rest} style={{ ...s, ...style }}>{Icon && <Icon size={16} />}{children}</button>;
};

/* --------- DOSYA İNDİRME --------- */
const handleDownload = async (id, filename, mimeType) => {
    try {
        const response = await api.get(`/api/store-artifacts/download/${id}`, { responseType: 'blob' });
        const url = window.URL.createObjectURL(new Blob([response.data], { type: mimeType }));
        const link = document.createElement('a');
        link.href = url;
        if (mimeType === 'application/pdf' || mimeType.startsWith('image/') || mimeType.startsWith('text/')) {
             link.target = "_blank";
        } else {
             link.setAttribute('download', filename);
        }
        document.body.appendChild(link);
        link.click();
        link.parentNode.removeChild(link);
        setTimeout(() => window.URL.revokeObjectURL(url), 1000);
    } catch (err) {
        console.error("Dosya indirilemedi:", err);
        alert("Dosya açılamadı.");
    }
};

/* --------- TABLO SATIRI --------- */
function ArtifactRow({ item, onClickVersions, onDelete, onEditTags }) {
    const tagsDisplay = item.tags && item.tags.length > 0
        ? item.tags.map(t => (typeof t === 'string' ? t : t.name)).join(", ")
        : "—";

    const folderName = item.folder ? `📁 ${item.folder.name}` : "";

    return (
        <div style={{ display: "grid", gridTemplateColumns: "6fr 3fr 3fr", alignItems: "center", padding: "12px 16px", borderBottom: `1px solid ${T.stroke}` }}>
            <div style={hStack(12, "flex-start", "center")}>
                <FileText size={18} color={T.muted} />
                <div style={vStack(4)}>
                    <div style={{ color: T.text, fontSize: 14 }}>
                        {item.filename}
                        {/* Güncel Versiyon Etiketi */}
                        <span style={{color: T.brand2, fontSize: 12, marginLeft: 8, background: 'rgba(34, 197, 94, 0.1)', padding: '2px 6px', borderRadius: 4}}>v{item.version} (Latest)</span>
                        <span style={{color: T.muted, fontSize: 12, marginLeft: 8}}>{folderName}</span>
                    </div>
                    <div style={{ color: T.muted, fontSize: 12 }}>{item.sizeLabel} • {item.mimeShort}</div>
                </div>
            </div>
            <div style={{ color: T.muted, fontSize: 13, display: "flex", alignItems: "center", gap: 8 }}>
                {tagsDisplay}
                <button onClick={() => onEditTags(item)} style={{ background: "transparent", border: "none", cursor: "pointer", padding: 4 }}>
                    <Edit size={14} color={T.brand} />
                </button>
            </div>
            <div style={{ ...hStack(12, "flex-end", "center") }}>
                <Button variant="ghost" icon={ExternalLink} onClick={() => handleDownload(item.id, item.filename, item.mimeType)}>Open</Button>
                <Button variant="subtle" icon={Layers} onClick={() => onClickVersions(item)}>Versions</Button>
                <Button variant="ghost" icon={Trash2} onClick={() => onDelete(item)}>Delete</Button>
            </div>
        </div>
    );
}

/* --------- VERSIONS DRAWER (GÜNCELLENDİ - ARTIK API'DEN ÇEKİYOR) --------- */
function VersionsDrawer({ open, onClose, filename, onMakeCurrent }) {
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(false);

    // Drawer açıldığında o dosyanın geçmişini çek
    useEffect(() => {
        if (open && filename) {
            fetchHistory();
        }
    }, [open, filename]);

    const fetchHistory = async () => {
        setLoading(true);
        try {
            // Backend'e eklediğimiz /history endpoint'i
            const res = await api.get(`/api/store-artifacts/history/${filename}`);
            setHistory(res.data);
        } catch (err) {
            console.error("Versiyon geçmişi alınamadı:", err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <AnimatePresence>
            {open && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} style={{ position: "fixed", inset: 0, zIndex: 50, display: "flex" }}>
                    <div onClick={onClose} style={{ flex: 1 }} />
                    <motion.aside initial={{ x: 400 }} animate={{ x: 0 }} exit={{ x: 400 }} style={{ width: 420, maxWidth: "88vw", height: "100%", borderLeft: `1px solid ${T.stroke}`, background: T.panel, display: "flex", flexDirection: "column" }}>
                        <div style={cardHeader}>
                            <div style={{ color: T.text, fontWeight: 600, fontSize: 14 }}>Version History – {filename}</div>
                            <button onClick={onClose} style={{ background: "transparent", border: "none" }}><X color={T.muted} /></button>
                        </div>
                        <div style={{ padding: 12, overflowY: "auto", flex: 1 }}>
                            {loading ? (
                                <div style={{color: T.muted, textAlign: 'center', padding: 20}}>Loading history...</div>
                            ) : history.length === 0 ? (
                                <div style={{color: T.muted, textAlign: 'center', padding: 20}}>No history found.</div>
                            ) : (
                                history.map(v => (
                                    <div key={v.id} style={{ border: `1px solid ${v.isCurrentVersion ? T.brand2 : T.stroke}`, borderRadius: 12, background: T.panelSoft, padding: 12, marginBottom: 10, position: 'relative' }}>
                                        
                                        <div style={hStack(12, "space-between", "flex-start")}>
                                            <div>
                                                <div style={{ ...hStack(6), marginBottom: 4 }}>
                                                    <span style={{ color: T.text, fontWeight: 'bold', fontSize: 15 }}>v{v.versionNumber}</span>
                                                    {v.isCurrentVersion && (
                                                        <span style={{ fontSize: 11, borderRadius: 12, padding: "2px 8px", background: T.brand2, color: "#000", fontWeight: 'bold' }}>
                                                            Current Version
                                                        </span>
                                                    )}
                                                </div>
                                                <div style={{ color: T.muted, fontSize: 12, display: 'flex', alignItems: 'center', gap: 4 }}>
                                                    <Clock size={12}/> {new Date(v.createdAt).toLocaleString()}
                                                </div>
                                            </div>
                                            
                                            <div style={vStack(8)}>
                                                <Button variant="ghost" icon={ExternalLink} style={{fontSize: 12, padding: "6px 10px"}} onClick={() => handleDownload(v.id, v.filename, v.mimeType)}>
                                                    Open
                                                </Button>
                                                
                                                {/* MAKE CURRENT BUTONU (Sadece güncel olmayanda çıkar) */}
                                                {!v.isCurrentVersion && (
                                                    <Button 
                                                        variant="subtle" 
                                                        icon={RefreshCw} 
                                                        style={{fontSize: 12, padding: "6px 10px", borderColor: T.brand}}
                                                        onClick={() => onMakeCurrent(v)}
                                                    >
                                                        Make Current
                                                    </Button>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </motion.aside>
                </motion.div>
            )}
        </AnimatePresence>
    );
}

/* --------- UPLOAD WIZARD (AYNI) --------- */
function UploadWizard({ open, onClose, onUploaded }) {
    const [file, setFile] = useState(null);
    const [step, setStep] = useState(1);
    const [msg, setMsg] = useState(null);
    
    const [tags, setTags] = useState("");
    const [folders, setFolders] = useState([]);
    const [selectedFolderId, setSelectedFolderId] = useState("");
    const [newFolderName, setNewFolderName] = useState("");
    const [showNewFolderInput, setShowNewFolderInput] = useState(false);
    const [showTagInput, setShowTagInput] = useState(false);
    const [showFolderSection, setShowFolderSection] = useState(false);

    useEffect(() => {
        if (open) fetchFolders();
    }, [open]);

    const fetchFolders = async () => {
        try {
            const res = await api.get("/api/folders");
            setFolders(res.data);
        } catch (err) { console.error("Klasörler alınamadı", err); }
    };

    const createFolder = async (e) => {
        if(e) e.preventDefault();
        if (!newFolderName.trim()) return;
        try {
            const res = await api.post("/api/folders", { name: newFolderName });
            setFolders([...folders, res.data]);
            setSelectedFolderId(res.data.id);
            setNewFolderName("");
            setShowNewFolderInput(false);
        } catch (err) {
            alert("Klasör oluşturulamadı: " + (err.response?.data?.error || err.message));
        }
    };

    const reset = () => { 
        setFile(null); setStep(1); setMsg(null); 
        setTags(""); setSelectedFolderId(""); 
        setShowTagInput(false); setShowFolderSection(false); setShowNewFolderInput(false);
    };

    const doUpload = async () => {
        if (!file) { setMsg({ t: "error", m: "Please choose a file." }); return; }
        try {
            setMsg({ t: "info", m: "Uploading…" });
            const localMimeType = file.type || "application/octet-stream";
            const fd = new FormData();
            fd.append("file", file);
            if (tags) fd.append("tags", tags);
            if (selectedFolderId) fd.append("folderId", selectedFolderId);

            // Upload sonrası sadece listeyi yenilemesi için onUploaded çağıracağız
            // Backend otomatik versionluyor.
            const { data } = await api.post("/api/store-artifacts/upload", fd, { headers: { "Content-Type": "multipart/form-data" } });

            setMsg({ t: "success", m: `Uploaded: ${data.filename} (v${data.versionNumber})` });
            
            // Yeni yüklenen dosya, listeyi yenilemesi için parent'a bildirilir
            onUploaded(); 
            
            setStep(3);
        } catch (err) {
            const isDup = err?.response?.status === 409;
            setMsg({ t: "error", m: isDup ? "This file content is identical to an existing version." : (err?.response?.data?.error || err.message) });
        }
    };

    return (
        <AnimatePresence>
            {open && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} style={{ position: "fixed", inset: 0, zIndex: 60, display: "flex", alignItems: "center", justifyContent: "center", background: "rgba(0,0,0,0.6)" , transition: { duration: 0.8 }}}>
                    <motion.div initial={{ y: 30, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 20, opacity: 0 }} style={{ width: 980, maxWidth: "95vw", borderRadius: 24, overflow: "hidden", ...card() }}>
                        <div style={cardHeader}>
                            <div style={{ color: T.text, fontWeight: 600 }}>Upload Artifact</div>
                            <button onClick={() => { reset(); onClose(); }} style={{ background: "transparent", border: "none" }}><X color={T.muted} /></button>
                        </div>
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
                                        <div style={{ ...hStack(8), flexWrap: "wrap" }}><span style={pill}>PDF</span><span style={pill}>Max 50MB</span><span style={pill}>Versioning ON</span></div>
                                    </div>
                                    <div style={{ gridColumn: "1 / span 2", ...hStack(8, "flex-end") }}><Button icon={ChevronRight} onClick={() => setStep(2)}>Next</Button></div>
                                </div>
                            )}
                            {step === 2 && (
                                <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 24 }}>
                                    <div style={vStack(16)}>
                                        <div style={vStack(8)}>
                                            <div style={{ color: T.text, fontSize: 14 }}>Tag & Categorize</div>
                                            <div style={hStack(8)}>
                                                <Button variant="subtle" icon={Tag} onClick={() => setShowTagInput(!showTagInput)}>{showTagInput ? "Hide Tag Input" : "Add tag"}</Button>
                                                <Button variant="subtle" icon={FolderPlus} onClick={() => setShowFolderSection(!showFolderSection)}>{showFolderSection ? "Hide Folder Select" : "Choose folder"}</Button>
                                            </div>
                                            {showTagInput && (<div style={{ marginTop: 8 }}><input autoFocus placeholder="Enter tags (e.g. java, v1)" value={tags} onChange={(e) => setTags(e.target.value)} style={{ ...inputBase, width: "100%", border: `1px solid ${T.brand}`, background: T.panelSoft, borderRadius: 12, padding: "10px 12px" }} /></div>)}
                                            {showFolderSection && (
                                                <div style={{ marginTop: 8, padding: 12, border: `1px solid ${T.stroke}`, borderRadius: 12, background: T.panelSoft }}>
                                                    <div style={{...hStack(8, "space-between"), marginBottom: 8}}>
                                                        <span style={{fontSize: 12, color: T.muted}}>Select Folder</span>
                                                        <button type="button" onClick={(e) => { e.preventDefault(); setShowNewFolderInput(!showNewFolderInput); }} style={{background:'none', border:'none', color: T.brand, fontSize:12, cursor:'pointer'}}>{showNewFolderInput ? "Cancel" : "+ New Folder"}</button>
                                                    </div>
                                                    {showNewFolderInput ? (
                                                        <div style={{display:'flex', gap:8}}>
                                                            <input placeholder="Folder Name" value={newFolderName} onChange={(e) => setNewFolderName(e.target.value)} style={{ ...inputBase, flex:1, border: `1px solid ${T.brand}`, background: T.panel, borderRadius: 8, padding: "8px 12px" }} />
                                                            <Button type="button" variant="solid" onClick={createFolder} style={{fontSize:12, padding:'6px 12px'}}>Add</Button>
                                                        </div>
                                                    ) : (
                                                        <select value={selectedFolderId} onChange={(e) => setSelectedFolderId(e.target.value)} style={{ ...inputBase, width: "100%", border: `1px solid ${T.stroke}`, background: T.panel, borderRadius: 8, padding: "8px 12px", cursor: "pointer" }}>
                                                            <option value="">-- Root (No Folder) --</option>
                                                            {folders.map(f => <option key={f.id} value={f.id}>{f.name}</option>)}
                                                        </select>
                                                    )}
                                                </div>
                                            )}
                                            {!showTagInput && !showFolderSection && (<div style={{ color: T.muted, fontSize: 12, marginTop: 4 }}>{tags ? `Tags: ${tags}` : "No tags."} {selectedFolderId ? `| Folder ID: ${selectedFolderId}` : ""}</div>)}
                                        </div>
                                    </div>
                                    <div style={{ gridColumn: "1 / span 2", ...hStack(8, "space-between") }}><Button variant="ghost" onClick={() => setStep(1)}>Back</Button><Button icon={PlusCircle} onClick={doUpload}>Next</Button></div>
                                </div>
                            )}
                            {step === 3 && (
                                <div style={{ border: `1px solid ${T.stroke}`, borderRadius: 12, background: T.panelSoft, padding: 12, ...hStack(12, "space-between") }}>
                                    <div style={hStack(12, "flex-start", "center")}><CheckCircle2 color={T.brand2} /><div><div style={{ color: T.text, fontSize: 14 }}>Stored & Indexed</div></div></div>
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

/* --------- BULK UPLOAD MODAL --------- */
function BulkUploadModal({ open, onClose, onUploaded }) {
    const [files, setFiles] = useState([]);
    const [tags, setTags] = useState("");
    const [folders, setFolders] = useState([]);
    const [selectedFolderId, setSelectedFolderId] = useState("");
    const [uploading, setUploading] = useState(false);
    const [results, setResults] = useState(null);

    useEffect(() => {
        if (open) fetchFolders();
    }, [open]);

    const fetchFolders = async () => {
        try {
            const res = await api.get("/api/folders");
            setFolders(res.data);
        } catch (err) { console.error("Klasörler alınamadı", err); }
    };

    const handleFileChange = (e) => {
        const selectedFiles = Array.from(e.target.files || []);
        setFiles(selectedFiles);
    };

    const doBulkUpload = async () => {
        if (files.length === 0) {
            alert("Lütfen en az bir dosya seçin.");
            return;
        }

        setUploading(true);
        setResults(null);

        try {
            const fd = new FormData();
            files.forEach(file => fd.append("files", file));
            if (tags) fd.append("tags", tags.split(",").map(t => t.trim()));
            if (selectedFolderId) fd.append("folderId", selectedFolderId);

            const { data } = await api.post("/api/store-artifacts/bulk-upload", fd, {
                headers: { "Content-Type": "multipart/form-data" }
            });

            setResults(data);
            onUploaded();
        } catch (err) {
            alert("Bulk upload failed: " + (err.response?.data?.error || err.message));
        } finally {
            setUploading(false);
        }
    };

    const reset = () => {
        setFiles([]);
        setTags("");
        setSelectedFolderId("");
        setResults(null);
    };

    return (
        <AnimatePresence>
            {open && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} style={{ position: "fixed", inset: 0, zIndex: 60, display: "flex", alignItems: "center", justifyContent: "center", background: "rgba(0,0,0,0.6)" }}>
                    <motion.div initial={{ y: 30, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 20, opacity: 0 }} style={{ width: 700, maxWidth: "95vw", borderRadius: 24, overflow: "hidden", ...card() }}>
                        <div style={cardHeader}>
                            <div style={{ color: T.text, fontWeight: 600 }}>Bulk Upload Artifacts</div>
                            <button onClick={() => { reset(); onClose(); }} style={{ background: "transparent", border: "none" }}><X color={T.muted} /></button>
                        </div>
                        <div style={{ padding: 24, display: "grid", gap: 16 }}>
                            {!results ? (
                                <>
                                    <div style={vStack(8)}>
                                        <div style={{ color: T.text, fontSize: 14 }}>Select Multiple Files</div>
                                        <label style={{ height: 120, borderRadius: 16, border: `1px solid ${T.stroke}`, background: T.panelSoft, ...hStack(8, "center", "center"), flexDirection: "column", cursor: "pointer" }}>
                                            <UploadCloud size={32} color={T.muted} />
                                            <div style={{ color: T.muted, fontSize: 14 }}>Choose files to upload</div>
                                            <input type="file" multiple style={{ display: "none" }} onChange={handleFileChange} />
                                        </label>
                                        <div style={{ color: T.muted, fontSize: 12 }}>
                                            {files.length > 0 ? `${files.length} file(s) selected` : "No files selected"}
                                        </div>
                                        {files.length > 0 && (
                                            <div style={{ maxHeight: 150, overflowY: "auto", border: `1px solid ${T.stroke}`, borderRadius: 8, padding: 8, background: T.panelSoft }}>
                                                {files.map((f, i) => (
                                                    <div key={i} style={{ color: T.text, fontSize: 12, padding: "4px 0" }}>
                                                        {i + 1}. {f.name} ({toSize(f.size)})
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>

                                    <div style={vStack(8)}>
                                        <div style={{ color: T.text, fontSize: 14 }}>Tags (comma-separated)</div>
                                        <input placeholder="e.g. java, v1, project-x" value={tags} onChange={(e) => setTags(e.target.value)} style={{ ...inputBase, width: "100%", border: `1px solid ${T.stroke}`, background: T.panelSoft, borderRadius: 12, padding: "10px 12px" }} />
                                    </div>

                                    <div style={vStack(8)}>
                                        <div style={{ color: T.text, fontSize: 14 }}>Folder (optional)</div>
                                        <select value={selectedFolderId} onChange={(e) => setSelectedFolderId(e.target.value)} style={{ ...inputBase, width: "100%", border: `1px solid ${T.stroke}`, background: T.panelSoft, borderRadius: 12, padding: "10px 12px", cursor: "pointer" }}>
                                            <option value="">-- No Folder --</option>
                                            {folders.map(f => <option key={f.id} value={f.id}>{f.name}</option>)}
                                        </select>
                                    </div>

                                    <div style={{ ...hStack(8, "flex-end") }}>
                                        <Button variant="ghost" onClick={() => { reset(); onClose(); }}>Cancel</Button>
                                        <Button icon={UploadCloud} onClick={doBulkUpload} disabled={uploading}>
                                            {uploading ? "Uploading..." : "Upload All"}
                                        </Button>
                                    </div>
                                </>
                            ) : (
                                <div style={vStack(12)}>
                                    <div style={{ color: T.text, fontSize: 16, fontWeight: 600 }}>Upload Results</div>
                                    <div style={{ ...hStack(16), fontSize: 14 }}>
                                        <span style={{ color: T.brand2 }}>✓ Success: {results.success}</span>
                                        <span style={{ color: "#EF4444" }}>✗ Failed: {results.failure}</span>
                                        <span style={{ color: "#F59E0B" }}>⊗ Duplicate: {results.duplicate}</span>
                                    </div>
                                    <div style={{ maxHeight: 300, overflowY: "auto", border: `1px solid ${T.stroke}`, borderRadius: 12, padding: 12, background: T.panelSoft }}>
                                        {results.results.map((r, i) => (
                                            <div key={i} style={{ padding: "8px 0", borderBottom: i < results.results.length - 1 ? `1px solid ${T.stroke}` : "none", fontSize: 13 }}>
                                                <div style={{ color: T.text }}>{r.filename}</div>
                                                <div style={{ color: r.status === 'success' ? T.brand2 : r.status === 'duplicate' ? '#F59E0B' : '#EF4444', fontSize: 12 }}>
                                                    {r.status === 'success' ? `✓ Uploaded (v${r.version})` : r.status === 'duplicate' ? '⊗ Duplicate' : `✗ ${r.message}`}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                    <Button onClick={() => { reset(); onClose(); }}>Close</Button>
                                </div>
                            )}
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
}

/* --------- BULK IMPORT MODAL --------- */
function BulkImportModal({ open, onClose, onUploaded }) {
    const [jsonData, setJsonData] = useState("");
    const [importing, setImporting] = useState(false);
    const [results, setResults] = useState(null);

    const doBulkImport = async () => {
        if (!jsonData.trim()) {
            alert("Please enter JSON data.");
            return;
        }

        setImporting(true);
        setResults(null);

        try {
            const parsedData = JSON.parse(jsonData);
            const { data } = await api.post("/api/store-artifacts/bulk-import", parsedData);
            setResults(data);
            onUploaded();
        } catch (err) {
            if (err instanceof SyntaxError) {
                alert("Invalid JSON format: " + err.message);
            } else {
                alert("Bulk import failed: " + (err.response?.data?.error || err.message));
            }
        } finally {
            setImporting(false);
        }
    };

    const reset = () => {
        setJsonData("");
        setResults(null);
    };

    const exampleJson = `{
  "artifacts": [
    {
      "filename": "example.txt",
      "mimeType": "text/plain",
      "data": "SGVsbG8gV29ybGQh",
      "tags": ["example", "test"],
      "folderId": null
    }
  ]
}`;

    return (
        <AnimatePresence>
            {open && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} style={{ position: "fixed", inset: 0, zIndex: 60, display: "flex", alignItems: "center", justifyContent: "center", background: "rgba(0,0,0,0.6)" }}>
                    <motion.div initial={{ y: 30, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 20, opacity: 0 }} style={{ width: 800, maxWidth: "95vw", borderRadius: 24, overflow: "hidden", ...card() }}>
                        <div style={cardHeader}>
                            <div style={{ color: T.text, fontWeight: 600 }}>Bulk Import from JSON</div>
                            <button onClick={() => { reset(); onClose(); }} style={{ background: "transparent", border: "none" }}><X color={T.muted} /></button>
                        </div>
                        <div style={{ padding: 24, display: "grid", gap: 16 }}>
                            {!results ? (
                                <>
                                    <div style={vStack(8)}>
                                        <div style={{ color: T.text, fontSize: 14 }}>JSON Data (Base64 encoded files)</div>
                                        <textarea
                                            placeholder={exampleJson}
                                            value={jsonData}
                                            onChange={(e) => setJsonData(e.target.value)}
                                            style={{ ...inputBase, width: "100%", minHeight: 300, border: `1px solid ${T.stroke}`, background: T.panelSoft, borderRadius: 12, padding: "12px", fontFamily: "monospace", fontSize: 12, resize: "vertical" }}
                                        />
                                        <div style={{ color: T.muted, fontSize: 11 }}>
                                            Note: "data" field should be Base64 encoded file content
                                        </div>
                                    </div>

                                    <div style={{ ...hStack(8, "flex-end") }}>
                                        <Button variant="ghost" onClick={() => { reset(); onClose(); }}>Cancel</Button>
                                        <Button icon={FileJson} onClick={doBulkImport} disabled={importing}>
                                            {importing ? "Importing..." : "Import"}
                                        </Button>
                                    </div>
                                </>
                            ) : (
                                <div style={vStack(12)}>
                                    <div style={{ color: T.text, fontSize: 16, fontWeight: 600 }}>Import Results</div>
                                    <div style={{ ...hStack(16), fontSize: 14 }}>
                                        <span style={{ color: T.brand2 }}>✓ Success: {results.success}</span>
                                        <span style={{ color: "#EF4444" }}>✗ Failed: {results.failure}</span>
                                    </div>
                                    <div style={{ maxHeight: 300, overflowY: "auto", border: `1px solid ${T.stroke}`, borderRadius: 12, padding: 12, background: T.panelSoft }}>
                                        {results.results.map((r, i) => (
                                            <div key={i} style={{ padding: "8px 0", borderBottom: i < results.results.length - 1 ? `1px solid ${T.stroke}` : "none", fontSize: 13 }}>
                                                <div style={{ color: T.text }}>{r.filename}</div>
                                                <div style={{ color: r.status === 'success' ? T.brand2 : r.status === 'duplicate' ? '#F59E0B' : '#EF4444', fontSize: 12 }}>
                                                    {r.status === 'success' ? `✓ Imported (v${r.version})` : r.status === 'duplicate' ? '⊗ Duplicate' : `✗ ${r.message}`}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                    <Button onClick={() => { reset(); onClose(); }}>Close</Button>
                                </div>
                            )}
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
}

/* --------- TAG EDIT MODAL --------- */
function TagEditModal({ open, onClose, artifact, onTagsUpdated }) {
    const [tags, setTags] = useState("");
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (open && artifact) {
            // Mevcut tag'leri input'a doldur
            const currentTags = artifact.tags && artifact.tags.length > 0
                ? artifact.tags.map(t => (typeof t === 'string' ? t : t.name)).join(", ")
                : "";
            setTags(currentTags);
        }
    }, [open, artifact]);

    const saveTagChanges = async () => {
        if (!artifact) return;

        setSaving(true);
        try {
            const tagArray = tags.split(",").map(t => t.trim()).filter(t => t.length > 0);
            await api.patch(`/api/store-artifacts/${artifact.id}/tags`, {
                tags: tagArray
            });
            onTagsUpdated();
            onClose();
        } catch (err) {
            alert("Failed to update tags: " + (err.response?.data?.error || err.message));
        } finally {
            setSaving(false);
        }
    };

    const reset = () => {
        setTags("");
    };

    return (
        <AnimatePresence>
            {open && artifact && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} style={{ position: "fixed", inset: 0, zIndex: 60, display: "flex", alignItems: "center", justifyContent: "center", background: "rgba(0,0,0,0.6)" }}>
                    <motion.div initial={{ y: 30, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 20, opacity: 0 }} style={{ width: 600, maxWidth: "95vw", borderRadius: 24, overflow: "hidden", ...card() }}>
                        <div style={cardHeader}>
                            <div style={{ color: T.text, fontWeight: 600 }}>Edit Tags - {artifact.filename}</div>
                            <button onClick={() => { reset(); onClose(); }} style={{ background: "transparent", border: "none" }}><X color={T.muted} /></button>
                        </div>
                        <div style={{ padding: 24, display: "grid", gap: 16 }}>
                            <div style={vStack(8)}>
                                <div style={{ color: T.text, fontSize: 14 }}>Tags (comma-separated)</div>
                                <input
                                    autoFocus
                                    placeholder="e.g. java, v1, project-x"
                                    value={tags}
                                    onChange={(e) => setTags(e.target.value)}
                                    style={{ ...inputBase, width: "100%", border: `1px solid ${T.brand}`, background: T.panelSoft, borderRadius: 12, padding: "12px" }}
                                />
                                <div style={{ color: T.muted, fontSize: 12 }}>
                                    Enter tags separated by commas. Leave empty to remove all tags.
                                </div>
                            </div>

                            <div style={{ ...hStack(8, "flex-end") }}>
                                <Button variant="ghost" onClick={() => { reset(); onClose(); }}>Cancel</Button>
                                <Button icon={Tag} onClick={saveTagChanges} disabled={saving}>
                                    {saving ? "Saving..." : "Save Tags"}
                                </Button>
                            </div>
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
}

/* --------- PAGE MAIN --------- */
export default function UploadArtifacts() {
    const [showWizard, setShowWizard] = useState(false);
    const [showBulkUpload, setShowBulkUpload] = useState(false);
    const [showBulkImport, setShowBulkImport] = useState(false);
    const [showTagEdit, setShowTagEdit] = useState(false);
    const [drawerOpen, setDrawerOpen] = useState(false);
    // Drawer'a dosya adını göndermemiz lazım (History için)
    const [drawerFilename, setDrawerFilename] = useState(null);
    const [selectedArtifactForTagEdit, setSelectedArtifactForTagEdit] = useState(null);
    const [rows, setRows] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    // Dosyaları Çek (Sadece Latest)
    const fetchArtifacts = async () => {
        setIsLoading(true);
        try {
            // Backend artık sadece isCurrentVersion=true olanları döndürüyor
            const { data } = await api.get("/api/store-artifacts/my-artifacts");
            const formattedRows = data.map(item => ({
                id: item.id,
                filename: item.filename,
                sizeLabel: toSize(item.sizeBytes),
                mimeShort: (item.mimeType || "file").split("/").pop()?.toUpperCase(),
                mimeType: item.mimeType,
                version: item.versionNumber,
                tags: item.tags || [],
                folder: item.folder
            }));
            setRows(formattedRows);
        } catch (err) { setRows([]); }
        setIsLoading(false);
    };

    useEffect(() => {
        fetchArtifacts();
    }, []);

    const createFolderPrompt = async () => {
        const folderName = prompt("Enter new folder name:");
        if (folderName && folderName.trim()) {
            try {
                await api.post("/api/folders", { name: folderName });
                alert(`Folder "${folderName}" created successfully!`);
            } catch (err) {
                alert("Failed to create folder: " + (err.response?.data?.error || err.message));
            }
        }
    };

    const onUploaded = () => {
        // Yeni yükleme yapıldığında listeyi yenile (Eski versiyon arşivlendi, yeni versiyon geldi)
        fetchArtifacts();
    };

    const openVersions = (item) => { 
        setDrawerFilename(item.filename); 
        setDrawerOpen(true); 
    };

    // --- MAKE CURRENT FONKSİYONU ---
    const handleMakeCurrent = async (versionItem) => {
        if (!window.confirm(`Are you sure you want to make version v${versionItem.versionNumber} the current version?`)) return;
        
        try {
            await api.put(`/api/store-artifacts/${versionItem.id}/make-current`);
            // İşlem başarılı olunca Drawer kapanır ve ana liste yenilenir.
            setDrawerOpen(false);
            fetchArtifacts();
        } catch (err) {
            alert("Failed to update version: " + (err.response?.data?.error || err.message));
        }
    };

    const doDelete = async (item) => {
        if (!window.confirm(`Delete "${item.filename}" (This Version)?`)) return;
        try {
            await api.delete(`/api/store-artifacts/${item.id}`);
            // Silindikten sonra listeyi yenile (Eğer current silindiyse yenisi current olabilir veya liste boşalır)
            fetchArtifacts();
        }
        catch (err) { alert("Silinemedi: " + err.message); }
    };

    const openTagEdit = (item) => {
        setSelectedArtifactForTagEdit(item);
        setShowTagEdit(true);
    };

    const onTagsUpdated = () => {
        // Tag'ler güncellendikten sonra listeyi yenile
        fetchArtifacts();
    };

    return (
        <div style={pageWrap}>
            <div style={container}>
                <div style={{ ...hStack(12, "space-between") }}>
                    <div style={{ fontSize: 28, fontWeight: 600, color: T.text }}>Dashboard</div>
                </div>
                <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 24, marginTop: 24 }}>
                    <div>
                        <div style={card(true)}>
                            <div style={cardHeader}><div style={{ color: T.text, fontSize: 14 }}>All artifacts</div></div>
                            <div style={cardBody}>
                                <div style={{ display: "grid", gridTemplateColumns: "6fr 3fr 3fr", padding: "10px 16px", fontSize: 12, color: T.muted, borderBottom: `1px solid ${T.stroke}` }}>
                                    <div>Name</div><div>Tags</div><div style={{ textAlign: "right" }}>Actions</div>
                                </div>
                                {isLoading ? <div style={{ padding: 16, color: T.muted }}>Loading...</div> : rows.map(r => <ArtifactRow key={r.id} item={r} onClickVersions={openVersions} onDelete={doDelete} onEditTags={openTagEdit} />)}
                            </div>
                        </div>
                    </div>
                    <div style={vStack(24)}>
                        <div style={card(true)}>
                            <div style={cardHeader}>
                                <div style={{ color: T.text, fontSize: 14 }}>Quick actions</div>
                                <Button variant="ghost" icon={SlidersHorizontal}>More</Button>
                            </div>
                            <div style={cardBody}>
                                <div style={{ ...hStack(8), flexWrap: "wrap" }}>
                                    <Button variant="subtle" icon={Upload} onClick={() => setShowWizard(true)}>Upload file</Button>
                                    <Button variant="subtle" icon={UploadCloud} onClick={() => setShowBulkUpload(true)}>Bulk Upload</Button>
                                    <Button variant="subtle" icon={FileJson} onClick={() => setShowBulkImport(true)}>Bulk Import</Button>
                                    <Button variant="subtle" icon={Tag} onClick={() => alert("Use upload wizard to add tags.")}>Tag</Button>
                                    <Button variant="subtle" icon={FolderPlus} onClick={createFolderPrompt}>New folder</Button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <UploadWizard open={showWizard} onClose={() => setShowWizard(false)} onUploaded={onUploaded} />
            <BulkUploadModal open={showBulkUpload} onClose={() => setShowBulkUpload(false)} onUploaded={onUploaded} />
            <BulkImportModal open={showBulkImport} onClose={() => setShowBulkImport(false)} onUploaded={onUploaded} />
            <TagEditModal open={showTagEdit} onClose={() => setShowTagEdit(false)} artifact={selectedArtifactForTagEdit} onTagsUpdated={onTagsUpdated} />

            {/* DRAWER GÜNCELLENDİ */}
            <VersionsDrawer
                open={drawerOpen}
                onClose={() => setDrawerOpen(false)}
                filename={drawerFilename}
                onMakeCurrent={handleMakeCurrent}
            />
        </div>
    );
}

function toSize(n) {
    if (n == null) return "—";
    const KB = 1024, MB = KB * 1024;
    if (n >= MB) return (n / MB).toFixed(1) + "MB";
    return Math.round(n / KB) + "KB";
}
