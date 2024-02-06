import { useMemo } from "react";

import { useRouter } from "next/router";
import { useSession } from "next-auth/react";

import { LinksType } from "@/utils/constant";
import { filterLinks, findActiveLink } from "@/utils/utils";


export function useLinks(links: LinksType) {
  const { pathname } = useRouter();
  const { data: session } = useSession();

  const activeLink = useMemo(() => findActiveLink(links, pathname), [pathname]);
  const filteredLinks = useMemo(() => filterLinks(links, session), [session]);

  return { activeLink, filteredLinks };
}
